package cz.mcsworld.eroded.death;

import com.mojang.authlib.GameProfile;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DeathChestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ErodedDeath");

    private record PendingChest(
            ServerLevel world,
            BlockPos pos,
            UUID playerUuid,
            GameProfile profile,
            List<ItemStack> items,
            long executeAtTick
    ) {}

    private static final List<PendingChest> PENDING_CHESTS = new ArrayList<>();

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                boolean isInvulnerable = player.isInvulnerableTo(player.level(), source);
                if (amount >= player.getHealth() && !isInvulnerable && !hasTotem(player)) {
                    handleDeath(player);
                }
            }
            return true;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long currentTick = server.getTickCount();
            Iterator<PendingChest> iterator = PENDING_CHESTS.iterator();

            while (iterator.hasNext()) {
                PendingChest pending = iterator.next();
                if (currentTick >= pending.executeAtTick()) {
                    createDeathChest(pending);
                    iterator.remove();
                }
            }
        });
    }

    private static boolean hasTotem(ServerPlayer player) {
        return player.getMainHandItem().is(Items.TOTEM_OF_UNDYING) ||
                player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
    }

    private static void handleDeath(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level();
        List<ItemStack> snapshot = new ArrayList<>();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                if (!stack.is(ErodedItems.DEATH_COMPASS)) {
                    snapshot.add(stack.copy());
                }
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        if (snapshot.isEmpty()) return;

        BlockPos deathPos = player.blockPosition().immutable();
        long executeAt = world.getServer().getTickCount() + 5;

        PENDING_CHESTS.add(new PendingChest(
                world,
                deathPos,
                player.getUUID(),
                player.getGameProfile(),
                snapshot,
                executeAt
        ));
    }

    private static void createDeathChest(PendingChest pending) {
        ServerLevel world = pending.world();
        BlockPos deathPos = pending.pos();
        UUID playerUuid = pending.playerUuid();

        try {
            BlockPos chestPos = findSurfacePos(world, deathPos);
            world.setBlock(chestPos, ErodedBlocks.DEATH_ENDER_CHEST.defaultBlockState(), 3);

            UUID hologramId = UUID.randomUUID();
            long deathValue = DeathValueCalculator.calculate(pending.items());

            ServerPlayer player = world.getServer().getPlayerList().getPlayer(playerUuid);

            long baseTimeMs;
            if (player != null) {
                baseTimeMs = DeathProtectionCalculator.calculateProtectionMillis(player, chestPos);
            } else {
                baseTimeMs = 300_000L;
            }

            long untilEpochMs = System.currentTimeMillis() + baseTimeMs;

            ErodedDeathMemory memory = new ErodedDeathMemory(
                    chestPos, world.dimension(), untilEpochMs, deathValue, hologramId
            );

            ErodedDeathStorage.putIfMoreValuable(playerUuid, memory);
            Map<Integer, DeathChestState.StoredStack> stored = DeathChestState.fromInventory(pending.items());
            DeathChestState.get(world).put(chestPos, playerUuid, untilEpochMs, stored, hologramId);

            if (player != null) {
                ErodedCompassHandler.onPlayerDeath(player, chestPos, true);
            }

            DeathHologramHandler.spawn(world, chestPos, pending.profile(), (int)(baseTimeMs / 1000), hologramId);

        } catch (Exception e) {
            LOGGER.error("Chyba při vytváření Death Chest: ", e);
        }
    }

    private static BlockPos findSurfacePos(ServerLevel world, BlockPos startPos) {
        BlockPos.MutableBlockPos mutable = startPos.mutable();
        while (world.getBlockState(mutable).isAir() && mutable.getY() > world.getMinY()) {
            mutable.move(0, -1, 0);
        }
        return (mutable.getY() < startPos.getY()) ? mutable.above().immutable() : startPos;
    }
}
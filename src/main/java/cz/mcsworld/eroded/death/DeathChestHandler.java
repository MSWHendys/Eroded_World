package cz.mcsworld.eroded.death;

import com.mojang.authlib.GameProfile;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
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
            ServerWorld world,
            BlockPos pos,
            UUID playerUuid,
            GameProfile profile,
            List<ItemStack> items,
            long executeAtTick
    ) {}

    private static final List<PendingChest> PENDING_CHESTS = new ArrayList<>();

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                boolean isInvulnerable = player.isInvulnerableTo(player.getWorld(), source);
                if (amount >= player.getHealth() && !isInvulnerable && !hasTotem(player)) {
                    handleDeath(player);
                }
            }
            return true;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long currentTick = server.getTicks();
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

    private static boolean hasTotem(ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) ||
                player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    private static void handleDeath(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        List<ItemStack> snapshot = new ArrayList<>();

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                if (!stack.isOf(ErodedItems.DEATH_COMPASS)) {
                    snapshot.add(stack.copy());
                }
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }

        if (snapshot.isEmpty()) return;

        BlockPos deathPos = player.getBlockPos().toImmutable();
        long executeAt = world.getServer().getTicks() + 5;

        PENDING_CHESTS.add(new PendingChest(
                world,
                deathPos,
                player.getUuid(),
                player.getGameProfile(),
                snapshot,
                executeAt
        ));
    }

    private static void createDeathChest(PendingChest pending) {
        ServerWorld world = pending.world();
        BlockPos deathPos = pending.pos();
        UUID playerUuid = pending.playerUuid();

        try {
            BlockPos chestPos = findSurfacePos(world, deathPos);
            world.setBlockState(chestPos, ErodedBlocks.DEATH_ENDER_CHEST.getDefaultState(), 3);

            UUID hologramId = UUID.randomUUID();
            long deathValue = DeathValueCalculator.calculate(pending.items());

            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerUuid);

            long baseTimeMs;
            if (player != null) {
                baseTimeMs = DeathProtectionCalculator.calculateProtectionMillis(player, chestPos);
            } else {
                baseTimeMs = 300_000L;
            }

            long untilEpochMs = System.currentTimeMillis() + baseTimeMs;

            ErodedDeathMemory memory = new ErodedDeathMemory(
                    chestPos, world.getRegistryKey(), untilEpochMs, deathValue, hologramId
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

    private static BlockPos findSurfacePos(ServerWorld world, BlockPos startPos) {
        BlockPos.Mutable mutable = startPos.mutableCopy();
        while (world.getBlockState(mutable).isAir() && mutable.getY() > world.getBottomY()) {
            mutable.move(0, -1, 0);
        }
        return (mutable.getY() < startPos.getY()) ? mutable.up().toImmutable() : startPos;
    }
}
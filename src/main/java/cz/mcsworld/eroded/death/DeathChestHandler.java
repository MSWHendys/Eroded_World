package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.core.ErodedItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DeathChestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ErodedDeath");

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {

                boolean isInvulnerable = player.isInvulnerableTo(player.getWorld(), source);

                if (amount >= player.getHealth() && !isInvulnerable && !hasTotem(player)) {
                    LOGGER.info("Detekováno fatální poškození pro {} (Příčina: {}). Zálohuji inventář.",
                            player.getName().getString(), source.getName());
                    handleDeath(player);
                }
            }
            return true;
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

        UUID hologramId = UUID.randomUUID();
        BlockPos deathPos = player.getBlockPos().toImmutable();
        UUID playerUuid = player.getUuid();
        var gameProfile = player.getGameProfile();

        new Thread(() -> {
            try {
                Thread.sleep(250);

                world.getServer().execute(() -> {
                    try {

                        BlockPos chestPos = findSurfacePos(world, deathPos);

                        world.setBlockState(chestPos, ErodedBlocks.DEATH_ENDER_CHEST.getDefaultState(), 3);

                        long deathValue = DeathValueCalculator.calculate(snapshot);
                        long baseTimeMs = DeathConfig.get().chest.protectionTicks * 50L;
                        long untilEpochMs = System.currentTimeMillis() + baseTimeMs;

                        ErodedDeathMemory memory = new ErodedDeathMemory(
                                chestPos, world.getRegistryKey(), untilEpochMs, deathValue, hologramId
                        );

                        ErodedDeathStorage.putIfMoreValuable(playerUuid, memory);
                        Map<Integer, DeathChestState.StoredStack> stored = DeathChestState.fromInventory(snapshot);
                        DeathChestState.get(world).put(chestPos, playerUuid, untilEpochMs, stored, hologramId);

                        ErodedCompassHandler.onPlayerDeath(player, chestPos, true);
                        DeathHologramHandler.spawn(world, chestPos, gameProfile, (int)(baseTimeMs/1000), hologramId);

                    } catch (Exception e) {
                        LOGGER.error("Chyba v odloženém zápisu: ", e);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static BlockPos findSurfacePos(ServerWorld world, BlockPos startPos) {
        BlockPos.Mutable mutable = startPos.mutableCopy();
        while (world.getBlockState(mutable).isAir() && mutable.getY() > world.getBottomY()) {
            mutable.move(0, -1, 0);
        }
        return (mutable.getY() < startPos.getY()) ? mutable.up().toImmutable() : startPos;
    }
}
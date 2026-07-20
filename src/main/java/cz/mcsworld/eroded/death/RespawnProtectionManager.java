package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Monster;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RespawnProtectionManager {

    private static final Map<UUID, ProtectionData> ACTIVE = new ConcurrentHashMap<>();
    private static long serverTick = 0;

    private RespawnProtectionManager() {
    }

    private record ProtectionData(long endTick) {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(RespawnProtectionManager::tick);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            DeathConfig cfg = DeathConfig.get();

            if (!cfg.respawnProtection.enabled || !cfg.respawnProtection.cancelOnAttack) {
                return InteractionResult.PASS;
            }

            if (!isProtected(serverPlayer)) {
                return InteractionResult.PASS;
            }

            end(serverPlayer);

            if (cfg.respawnProtection.showMessage) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("eroded.respawn_protection.cancelled_by_attack"),
                        true
                );
            }

            return InteractionResult.FAIL;
        });
    }

    public static void start(ServerPlayer player) {
        DeathConfig cfg = DeathConfig.get();

        if (!cfg.respawnProtection.enabled) {
            return;
        }

        int duration = Math.max(1, cfg.respawnProtection.durationTicks);

        ACTIVE.put(
                player.getUUID(),
                new ProtectionData(serverTick + duration)
        );

        if (cfg.respawnProtection.showMessage) {
            player.sendSystemMessage(
                    Component.translatable("eroded.respawn_protection.started"),
                    true
            );
        }

        if (cfg.respawnProtection.clearMobTargets) {
            clearNearbyMobTargets(player, cfg.respawnProtection.clearTargetRadius);
        }
    }

    public static boolean isProtected(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static boolean shouldPreventDamage(ServerPlayer player) {
        DeathConfig cfg = DeathConfig.get();

        return cfg.respawnProtection.enabled
                && cfg.respawnProtection.preventDamage
                && isProtected(player);
    }

    public static void end(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    private static void tick(MinecraftServer server) {
        serverTick++;

        DeathConfig cfg = DeathConfig.get();

        Iterator<Map.Entry<UUID, ProtectionData>> iterator = ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ProtectionData> entry = iterator.next();

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }

            ProtectionData data = entry.getValue();

            if (serverTick >= data.endTick()) {
                iterator.remove();

                if (cfg.respawnProtection.showMessage) {
                    player.sendSystemMessage(
                            Component.translatable("eroded.respawn_protection.expired"),
                            true
                    );
                }

                continue;
            }

            if (cfg.respawnProtection.clearMobTargets) {
                clearNearbyMobTargets(player, cfg.respawnProtection.clearTargetRadius);
            }
        }
    }

    private static void clearNearbyMobTargets(ServerPlayer player, double radius) {
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }

        double safeRadius = Math.max(1.0, radius);

        for (Monster mob : world.getEntitiesOfClass(
                Monster.class,
                player.getBoundingBox().inflate(safeRadius),
                mob -> mob.isAlive() && mob.getTarget() == player
        )) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }
}
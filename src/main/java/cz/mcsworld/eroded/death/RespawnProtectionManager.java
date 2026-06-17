package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

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
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            DeathConfig cfg = DeathConfig.get();

            if (!cfg.respawnProtection.enabled || !cfg.respawnProtection.cancelOnAttack) {
                return ActionResult.PASS;
            }

            if (!isProtected(serverPlayer)) {
                return ActionResult.PASS;
            }

            end(serverPlayer);

            if (cfg.respawnProtection.showMessage) {
                serverPlayer.sendMessage(
                        Text.translatable("eroded.respawn_protection.cancelled_by_attack"),
                        true
                );
            }

            return ActionResult.FAIL;
        });
    }

    public static void start(ServerPlayerEntity player) {
        DeathConfig cfg = DeathConfig.get();

        if (!cfg.respawnProtection.enabled) {
            return;
        }

        int duration = Math.max(1, cfg.respawnProtection.durationTicks);

        ACTIVE.put(
                player.getUuid(),
                new ProtectionData(serverTick + duration)
        );

        if (cfg.respawnProtection.showMessage) {
            player.sendMessage(
                    Text.translatable("eroded.respawn_protection.started"),
                    true
            );
        }

        if (cfg.respawnProtection.clearMobTargets) {
            clearNearbyMobTargets(player, cfg.respawnProtection.clearTargetRadius);
        }
    }

    public static boolean isProtected(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUuid());
    }

    public static boolean shouldPreventDamage(ServerPlayerEntity player) {
        DeathConfig cfg = DeathConfig.get();

        return cfg.respawnProtection.enabled
                && cfg.respawnProtection.preventDamage
                && isProtected(player);
    }

    public static void end(ServerPlayerEntity player) {
        ACTIVE.remove(player.getUuid());
    }

    private static void tick(MinecraftServer server) {
        serverTick++;

        DeathConfig cfg = DeathConfig.get();

        Iterator<Map.Entry<UUID, ProtectionData>> iterator = ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ProtectionData> entry = iterator.next();

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }

            ProtectionData data = entry.getValue();

            if (serverTick >= data.endTick()) {
                iterator.remove();

                if (cfg.respawnProtection.showMessage) {
                    player.sendMessage(
                            Text.translatable("eroded.respawn_protection.expired"),
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

    private static void clearNearbyMobTargets(ServerPlayerEntity player, double radius) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }

        double safeRadius = Math.max(1.0, radius);

        for (HostileEntity mob : world.getEntitiesByClass(
                HostileEntity.class,
                player.getBoundingBox().expand(safeRadius),
                mob -> mob.isAlive() && mob.getTarget() == player
        )) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }
}
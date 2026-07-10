package cz.mcsworld.eroded.combat;

import cz.mcsworld.eroded.config.combat.CombatConfig;
import cz.mcsworld.eroded.network.DodgeRequestPacket;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DodgeHandler {

    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private DodgeHandler() {}

    public static void register() {

        ServerPlayNetworking.registerGlobalReceiver(
                DodgeRequestPacket.ID,
                (payload, context) -> {
                    ServerPlayer player = context.player();

                    context.server().execute(() ->
                            handle(player, payload)
                    );
                }
        );
    }

    private static void handle(ServerPlayer player, DodgeRequestPacket pkt) {
        var root = CombatConfig.get();
        if (!root.enabled || !root.dodge.enabled) return;

        var cfg = root.dodge;

        if (!cfg.allowBackward && pkt.dirZ() < 0) return;
        if (!cfg.allowSideways && pkt.dirX() != 0) return;

        UUID id = player.getUUID();

        long ticks = player.level().getGameTime();

        long last = COOLDOWNS.getOrDefault(id, -9999L);
        if (ticks - last < cfg.cooldownTicks) return;

        SkillData data = SkillManager.get(player);

        Vec3 dir = resolveDirection(player, pkt);
        Vec3 start = player.position();

        Vec3 safeTarget = findSafeTarget(
                player,
                start,
                dir,
                cfg.maxDistance,
                cfg.stepSize
        );

        if (safeTarget == null) return;

        if (!data.tryConsumeEnergy(cfg.energyCost)) {
            return;
        }

        SkillManager.save(player);

        player.teleportTo(
                safeTarget.x,
                player.getY(),
                safeTarget.z
        );

        COOLDOWNS.put(id, ticks);
    }

    private static Vec3 resolveDirection(ServerPlayer player, DodgeRequestPacket pkt) {

        float yaw = player.getYRot();
        float rad = yaw * Mth.DEG_TO_RAD;

        Vec3 forward = new Vec3(-Mth.sin(rad), 0, Mth.cos(rad));
        Vec3 right = new Vec3(Mth.cos(rad), 0, Mth.sin(rad));

        if (pkt.dirZ() > 0) return forward;
        if (pkt.dirZ() < 0) return forward.reverse();
        if (pkt.dirX() > 0) return right;

        return right.reverse();
    }

    private static Vec3 findSafeTarget(
            ServerPlayer player,
            Vec3 start,
            Vec3 dir,
            double maxDistance,
            double step
    ) {

        AABB box = player.getBoundingBox();
        Vec3 lastSafe = start;

        for (double d = step; d <= maxDistance; d += step) {

            Vec3 pos = start.add(dir.scale(d));

            AABB moved = box.move(
                    pos.x - start.x,
                    0,
                    pos.z - start.z
            );

            if (!player.level().noCollision(player, moved)) {
                break;
            }

            lastSafe = pos;
        }

        return lastSafe.equals(start) ? null : lastSafe;
    }

    public static void cleanup(UUID playerId) {
        COOLDOWNS.remove(playerId);
    }
}
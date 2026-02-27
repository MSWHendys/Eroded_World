package cz.mcsworld.eroded.combat;

import cz.mcsworld.eroded.config.combat.CombatConfig;
import cz.mcsworld.eroded.network.DodgeRequestPacket;
import cz.mcsworld.eroded.network.EnergyWarningPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DodgeHandler {

    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

    private DodgeHandler() {}

    public static void register() {

        ServerPlayNetworking.registerGlobalReceiver(
                DodgeRequestPacket.ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() ->
                            handle(player, payload)
                    );
                }
        );
    }

    private static void handle(ServerPlayerEntity player, DodgeRequestPacket pkt) {
        var root = CombatConfig.get();
        if (!root.enabled || !root.dodge.enabled) return;
        var cfg = root.dodge;

        if (!cfg.allowBackward && pkt.dirZ() < 0) return;
        if (!cfg.allowSideways && pkt.dirX() != 0) return;

        UUID id = player.getUuid();
        int ticks = player.getServer().getTicks();

        int last = COOLDOWNS.getOrDefault(id, -9999);
        if (ticks - last < cfg.cooldownTicks) return;

        SkillData data = SkillManager.get(player);
        if (!data.tryConsumeEnergy(cfg.energyCost)) {
            return;
        }

        Vec3d dir = resolveDirection(player, pkt);
        Vec3d start = player.getPos();
        Vec3d safeTarget = findSafeTarget(
                player,
                start,
                dir,
                cfg.maxDistance,
                cfg.stepSize
        );

        if (safeTarget == null) return;

        player.requestTeleport(
                safeTarget.x,
                player.getY(),
                safeTarget.z
        );

        COOLDOWNS.put(id, ticks);
    }

    private static Vec3d resolveDirection(ServerPlayerEntity player, DodgeRequestPacket pkt) {

        float yaw = player.getYaw();
        float rad = yaw * MathHelper.RADIANS_PER_DEGREE;

        Vec3d forward = new Vec3d(-MathHelper.sin(rad), 0, MathHelper.cos(rad));
        Vec3d right   = new Vec3d(MathHelper.cos(rad), 0, MathHelper.sin(rad));

        if (pkt.dirZ() > 0) return forward;
        if (pkt.dirZ() < 0) return forward.negate();
        if (pkt.dirX() > 0) return right;
        return right.negate();
    }

    private static Vec3d findSafeTarget(
            ServerPlayerEntity player,
            Vec3d start,
            Vec3d dir,
            double maxDistance,
            double step
    ) {

        Box box = player.getBoundingBox();
        Vec3d lastSafe = start;

        for (double d = step; d <= maxDistance; d += step) {

            Vec3d pos = start.add(dir.multiply(d));
            Box moved = box.offset(
                    pos.x - start.x,
                    0,
                    pos.z - start.z
            );

            if (!player.getWorld().isSpaceEmpty(player, moved)) break;
            lastSafe = pos;
        }

        return lastSafe.equals(start) ? null : lastSafe;
    }
}

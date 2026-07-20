package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class DeathProtectionCalculator {

    private DeathProtectionCalculator() {}

    public static long calculateProtectionMillis(ServerPlayer player, BlockPos deathPos) {

        var cfg = DeathConfig.get().protection;

        if (!cfg.distanceBased) {
            return DeathConfig.get().chest.protectionTicks * 50L;
        }

        ServerLevel world = (ServerLevel) player.level();

        BlockPos respawnPos = null;

        var respawn = player.getRespawnConfig();
        if (respawn != null) {
            if (respawn.dimension().equals(world.dimension())) {
                respawnPos = respawn.pos();
            }
        }

        if (respawnPos == null && cfg.useSpawnIfNoBed) {
            respawnPos = world.getSharedSpawnPos();
        }

        if (respawnPos == null) {
            return cfg.minMinutes * 60L * 1000L;
        }

        double distance = Math.sqrt(deathPos.distSqr(respawnPos));

        double minutes = cfg.minMinutes + (distance * cfg.minutesPerBlock);
        minutes = Math.min(minutes, cfg.maxMinutes);

        return (long)(minutes * 60_000L);
    }
}
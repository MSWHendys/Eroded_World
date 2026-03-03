package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class DeathProtectionCalculator {

    private DeathProtectionCalculator() {}

    public static long calculateProtectionMillis(ServerPlayerEntity player, BlockPos deathPos) {

        var cfg = DeathConfig.get().protection;

        if (!cfg.distanceBased) {
            return DeathConfig.get().chest.protectionTicks * 50L;
        }

        ServerWorld world = (ServerWorld) player.getWorld();

        BlockPos respawnPos = null;

        var respawn = player.getRespawn();
        if (respawn != null) {
            if (respawn.dimension().equals(world.getRegistryKey())) {
                respawnPos = respawn.pos();
            }
        }

        if (respawnPos == null && cfg.useSpawnIfNoBed) {
            respawnPos = world.getSpawnPos();
        }

        if (respawnPos == null) {
            return cfg.minMinutes * 60L * 1000L;
        }

        double distance = Math.sqrt(deathPos.getSquaredDistance(respawnPos));

        double minutes = cfg.minMinutes + (distance * cfg.minutesPerBlock);
        minutes = Math.min(minutes, cfg.maxMinutes);

        return (long)(minutes * 60_000L);
    }
}
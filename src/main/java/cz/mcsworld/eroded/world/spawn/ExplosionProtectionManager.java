package cz.mcsworld.eroded.world.spawn;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class ExplosionProtectionManager {

    private ExplosionProtectionManager() {}

    private static TerritoryConfig.Server cfg() {
        return TerritoryConfig.get().server;
    }

    public static boolean isSpawnProtectionEnabled() {
        return cfg().enabled && cfg().spawnProtectionEnabled;
    }

    public static boolean isExplosionProtectionEnabled() {
        return cfg().preventExplosions;
    }

    public static boolean isProtected(ServerWorld world, BlockPos pos) {

        if (!isSpawnProtectionEnabled()) {
            return false;
        }

        BlockPos spawn = world.getSpawnPos();

        int radius = cfg().spawnProtectionRadius;

        int dx = Math.abs(pos.getX() - spawn.getX());
        int dz = Math.abs(pos.getZ() - spawn.getZ());

        return dx <= radius && dz <= radius;
    }

    public static boolean canBreak(ServerPlayerEntity player, BlockPos pos) {

        if (hasBypass(player)) {
            return true;
        }

        if (!cfg().preventBlockBreak) {
            return true;
        }

        return !isProtected(player.getWorld(), pos);
    }

    public static boolean canPlace(ServerPlayerEntity player, BlockPos pos) {

        if (hasBypass(player)) {
            return true;
        }

        if (!cfg().preventBlockPlace) {
            return true;
        }

        return !isProtected(player.getWorld(), pos);
    }

    public static boolean preventPistonPush() {
        return cfg().preventPistonPush;
    }

    private static boolean hasBypass(ServerPlayerEntity player) {

        if (cfg().bypassCreative && player.isCreative()) {
            return true;
        }

        if (cfg().bypassOP && player.hasPermissionLevel(2)) {
            return true;
        }

        return false;
    }
}
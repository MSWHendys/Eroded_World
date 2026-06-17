package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class MobGriefingProtectionManager {

    private MobGriefingProtectionManager() {
    }

    private static TerritoryConfig.Server cfg() {
        return TerritoryConfig.get().server;
    }

    private static boolean isClaimMobGriefingProtectionEnabled() {
        return cfg().enabled
                && cfg().playerClaimProtectionEnabled
                && cfg().protectClaimMobGriefing;
    }

    private static boolean isSpawnMobGriefingProtectionEnabled() {
        return ExplosionProtectionManager.preventMobGriefing();
    }

    public static boolean isProtected(ServerWorld world, BlockPos pos) {

        if (isSpawnMobGriefingProtectionEnabled()
                && ExplosionProtectionManager.isProtected(world, pos)) {
            return true;
        }

        if (!isClaimMobGriefingProtectionEnabled()) {
            return false;
        }

        return TerritoryProtectionManager.getAnchorClaim(world, pos) != null
                || TerritoryProtectionManager.getActiveClaimAt(world, pos) != null;
    }

    public static boolean isProtectedAround(Entity entity, int radius) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return false;
        }

        BlockPos center = entity.getBlockPos();
        int r = Math.max(0, radius);

        for (int x = -r; x <= r; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.add(x, y, z);

                    if (isProtected(world, pos)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean canMobModifyAt(ServerWorld world, BlockPos pos) {
        return !isProtected(world, pos);
    }
}
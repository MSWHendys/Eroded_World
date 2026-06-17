package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class ProjectileProtectionManager {

    private ProjectileProtectionManager() {
    }

    private static TerritoryConfig.Server cfg() {
        return TerritoryConfig.get().server;
    }

    public static boolean canProjectileMoveBetween(
            ServerWorld world,
            BlockPos fromPos,
            BlockPos toPos
    ) {
        if (!cfg().enabled) {
            return true;
        }

        if (fromPos.equals(toPos)) {
            return true;
        }

        if (crossesSpawnBoundary(world, fromPos, toPos)) {
            return !ExplosionProtectionManager.preventProjectileBoundaryActions();
        }

        if (!cfg().preventProjectileBoundaryActions) {
            return true;
        }

        return ProtectionBoundaryManager.canAutomationMoveBetween(
                world,
                fromPos,
                toPos
        );
    }

    public static boolean canProjectileAffect(
            ServerWorld world,
            Entity projectile,
            BlockPos targetPos
    ) {
        if (!cfg().enabled) {
            return true;
        }

        return canProjectileMoveBetween(
                world,
                projectile.getBlockPos(),
                targetPos
        );
    }

    private static boolean crossesSpawnBoundary(
            ServerWorld world,
            BlockPos fromPos,
            BlockPos toPos
    ) {
        boolean fromSpawn = ExplosionProtectionManager.isProtected(world, fromPos);
        boolean toSpawn = ExplosionProtectionManager.isProtected(world, toPos);

        return fromSpawn != toSpawn;
    }
}
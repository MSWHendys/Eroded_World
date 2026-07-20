package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class ProjectileProtectionManager {

    private ProjectileProtectionManager() {
    }

    private static TerritoryConfig.Server cfg() {
        return TerritoryConfig.get().server;
    }

    public static boolean canProjectileMoveBetween(
            ServerLevel world,
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
            ServerLevel world,
            Entity projectile,
            BlockPos targetPos
    ) {
        if (!cfg().enabled) {
            return true;
        }

        return canProjectileMoveBetween(
                world,
                projectile.blockPosition(),
                targetPos
        );
    }

    private static boolean crossesSpawnBoundary(
            ServerLevel world,
            BlockPos fromPos,
            BlockPos toPos
    ) {
        boolean fromSpawn = ExplosionProtectionManager.isProtected(world, fromPos);
        boolean toSpawn = ExplosionProtectionManager.isProtected(world, toPos);

        return fromSpawn != toSpawn;
    }
}
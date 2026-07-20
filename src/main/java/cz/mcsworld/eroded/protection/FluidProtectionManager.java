package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class FluidProtectionManager {

    private FluidProtectionManager() {
    }

    private static TerritoryConfig.Server cfg() {
        return TerritoryConfig.get().server;
    }

    private static boolean isClaimFluidFlowProtectionEnabled() {
        return cfg().enabled
                && cfg().playerClaimProtectionEnabled
                && cfg().protectClaimFluidFlow;
    }

    public static boolean canFluidFlow(
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
            return !ExplosionProtectionManager.preventFluidFlow();
        }

        if (!isClaimFluidFlowProtectionEnabled()) {
            return true;
        }

        return ProtectionBoundaryManager.canAutomationMoveBetween(
                world,
                fromPos,
                toPos
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
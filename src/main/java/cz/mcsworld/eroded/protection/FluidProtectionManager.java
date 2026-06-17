package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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
            ServerWorld world,
            BlockPos fromPos,
            BlockPos toPos
    ) {
        boolean fromSpawn = ExplosionProtectionManager.isProtected(world, fromPos);
        boolean toSpawn = ExplosionProtectionManager.isProtected(world, toPos);

        return fromSpawn != toSpawn;
    }
}
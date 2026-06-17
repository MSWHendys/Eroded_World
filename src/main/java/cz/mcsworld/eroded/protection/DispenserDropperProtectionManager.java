package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class DispenserDropperProtectionManager {

    private DispenserDropperProtectionManager() {
    }

    private static TerritoryConfig.Server cfg() {
        return TerritoryConfig.get().server;
    }

    private static boolean isClaimDispenserDropperProtectionEnabled() {
        return cfg().enabled
                && cfg().playerClaimProtectionEnabled
                && cfg().protectClaimDispenserDropperBoundaryActions;
    }

    public static boolean canDispense(
            ServerWorld world,
            BlockPos sourcePos,
            BlockState state
    ) {
        if (!cfg().enabled) {
            return true;
        }

        if (!state.contains(DispenserBlock.FACING)) {
            return true;
        }

        Direction direction = state.get(DispenserBlock.FACING);
        BlockPos targetPos = sourcePos.offset(direction);

        if (crossesSpawnBoundary(world, sourcePos, targetPos)) {
            return !ExplosionProtectionManager.preventDispenserDropperBoundaryActions();
        }

        if (!isClaimDispenserDropperProtectionEnabled()) {
            return true;
        }

        return ProtectionBoundaryManager.canAutomationMoveBetween(
                world,
                sourcePos,
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
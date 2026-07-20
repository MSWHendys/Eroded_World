package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

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
            ServerLevel world,
            BlockPos sourcePos,
            BlockState state
    ) {
        if (!cfg().enabled) {
            return true;
        }

        if (!state.hasProperty(DispenserBlock.FACING)) {
            return true;
        }

        Direction direction = state.getValue(DispenserBlock.FACING);
        BlockPos targetPos = sourcePos.relative(direction);

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
            ServerLevel world,
            BlockPos fromPos,
            BlockPos toPos
    ) {
        boolean fromSpawn = ExplosionProtectionManager.isProtected(world, fromPos);
        boolean toSpawn = ExplosionProtectionManager.isProtected(world, toPos);

        return fromSpawn != toSpawn;
    }
}
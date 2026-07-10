package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.ProtectionBoundaryManager;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public abstract class PistonProtectionMixin {

    @Inject(
            method = "moveBlocks",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$protectPistonBoundaryMove(
            Level world,
            BlockPos pos,
            Direction direction,
            boolean extend,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }

        if (!ExplosionProtectionManager.preventPistonPush()) {
            return;
        }

        PistonStructureResolver handler = new PistonStructureResolver(
                world,
                pos,
                direction,
                extend
        );

        if (!handler.resolve()) {
            return;
        }

        Direction moveDirection = extend
                ? direction
                : direction.getOpposite();

        for (BlockPos movedPos : handler.getToPush()) {
            BlockPos targetPos = movedPos.relative(moveDirection);

            if (!ProtectionBoundaryManager.canPistonMoveBlock(
                    serverWorld,
                    movedPos,
                    targetPos
            )) {
                cir.setReturnValue(false);
                return;
            }
        }

        for (BlockPos brokenPos : handler.getToDestroy()) {
            if (!ProtectionBoundaryManager.canPistonBreakBlock(
                    serverWorld,
                    brokenPos
            )) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
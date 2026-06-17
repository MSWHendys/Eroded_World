package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.ProtectionBoundaryManager;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public abstract class PistonProtectionMixin {

    @Inject(
            method = "move",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$protectPistonBoundaryMove(
            World world,
            BlockPos pos,
            Direction direction,
            boolean extend,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!ExplosionProtectionManager.preventPistonPush()) {
            return;
        }

        PistonHandler handler = new PistonHandler(
                world,
                pos,
                direction,
                extend
        );

        if (!handler.calculatePush()) {
            return;
        }

        Direction moveDirection = extend
                ? direction
                : direction.getOpposite();

        for (BlockPos movedPos : handler.getMovedBlocks()) {
            BlockPos targetPos = movedPos.offset(moveDirection);

            if (!ProtectionBoundaryManager.canPistonMoveBlock(
                    serverWorld,
                    movedPos,
                    targetPos
            )) {
                cir.setReturnValue(false);
                return;
            }
        }

        for (BlockPos brokenPos : handler.getBrokenBlocks()) {
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
package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.FluidProtectionManager;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowableFluid.class)
public abstract class FluidFlowProtectionMixin {

    @Inject(
            method = "flow",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventFluidCrossProtectionBorder(
            WorldAccess world,
            BlockPos pos,
            BlockState state,
            Direction direction,
            FluidState fluidState,
            CallbackInfo ci
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        BlockPos fromPos = pos.offset(direction.getOpposite());

        if (!FluidProtectionManager.canFluidFlow(
                serverWorld,
                fromPos,
                pos
        )) {
            ci.cancel();
        }
    }
}
package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.FluidProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public abstract class FluidFlowProtectionMixin {

    @Inject(
            method = "spreadTo",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventFluidCrossProtectionBorder(
            LevelAccessor world,
            BlockPos pos,
            BlockState state,
            Direction direction,
            FluidState fluidState,
            CallbackInfo ci
    ) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }

        BlockPos fromPos = pos.relative(direction.getOpposite());

        if (!FluidProtectionManager.canFluidFlow(
                serverWorld,
                fromPos,
                pos
        )) {
            ci.cancel();
        }
    }
}
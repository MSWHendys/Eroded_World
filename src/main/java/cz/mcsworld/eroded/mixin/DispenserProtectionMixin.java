package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.DispenserDropperProtectionManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public abstract class DispenserProtectionMixin {

    @Inject(
            method = "dispense",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventDispenserAcrossProtectionBoundary(
            ServerWorld world,
            BlockState state,
            BlockPos pos,
            CallbackInfo ci
    ) {
        if (!DispenserDropperProtectionManager.canDispense(world, pos, state)) {
            ci.cancel();
        }
    }
}
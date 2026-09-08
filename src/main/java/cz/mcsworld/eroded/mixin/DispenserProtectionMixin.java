package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.DispenserDropperProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public abstract class DispenserProtectionMixin {

    @Inject(
            method = "dispenseFrom",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventDispenserAcrossProtectionBoundary(
            ServerLevel world,
            BlockState state,
            BlockPos pos,
            CallbackInfo ci
    ) {
        if (DispenserDropperProtectionManager.canDispense(world, pos, state)) {
            ci.cancel();
        }
    }
}
package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonHandler.class)
public abstract class PistonProtectionMixin {

    @Shadow
    private ServerWorld world;

    @Inject(
            method = "tryMove",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventPistonPush(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (!ExplosionProtectionManager.preventPistonPush()) return;

        if (ExplosionProtectionManager.isProtected(world, pos)) {
            cir.setReturnValue(false);
        }
    }
}
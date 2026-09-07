package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.entity.ErodedSpecialSkeletonEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft 1.21.11 moved Mob#isSunBurnTick() to a private method, so the
 * Eroded skeleton can no longer override it directly. Cancel vanilla sunlight
 * burning for the custom skeleton here; ErodedMobSunBehaviour owns its burn
 * timing exactly as in the rc.1 implementation.
 */
@Mixin(Mob.class)
public abstract class MobSunBurnMixin {

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void eroded$disableVanillaSunBurn(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ErodedSpecialSkeletonEntity) {
            cir.setReturnValue(false);
        }
    }
}

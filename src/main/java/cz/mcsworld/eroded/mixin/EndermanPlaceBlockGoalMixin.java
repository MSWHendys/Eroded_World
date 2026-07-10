package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.MobGriefingProtectionManager;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal")
public abstract class EndermanPlaceBlockGoalMixin {

    @Shadow
    @Final
    private EnderMan enderman;

    @Inject(
            method = "canUse",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventEndermanPlacingBlocksInClaims(
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (MobGriefingProtectionManager.isProtectedAround(enderman, 4)) {
            cir.setReturnValue(false);
        }
    }
}
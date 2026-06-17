package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.MobGriefingProtectionManager;
import net.minecraft.entity.mob.EndermanEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.entity.mob.EndermanEntity$PickUpBlockGoal")
public abstract class EndermanPickUpBlockGoalMixin {

    @Shadow
    @Final
    private EndermanEntity enderman;

    @Inject(
            method = "canStart",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventEndermanPickingUpClaimBlocks(
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (MobGriefingProtectionManager.isProtectedAround(enderman, 4)) {
            cir.setReturnValue(false);
        }
    }
}
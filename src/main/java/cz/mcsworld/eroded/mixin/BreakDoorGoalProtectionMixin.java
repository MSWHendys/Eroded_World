package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.MobGriefingProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalProtectionMixin {

    @Inject(
            method = "canUse",
            at = @At("RETURN"),
            cancellable = true
    )
    private void eroded$preventStartingDoorBreakInClaim(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) {
            return;
        }

        if (isDoorProtected()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "canContinueToUse",
            at = @At("RETURN"),
            cancellable = true
    )
    private void eroded$preventContinuingDoorBreakInClaim(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) {
            return;
        }

        if (isDoorProtected()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventDoorBreakTickInClaim(CallbackInfo ci) {
        if (isDoorProtected()) {
            ci.cancel();
        }
    }

    private boolean isDoorProtected() {
        DoorInteractGoalAccessor accessor =
                (DoorInteractGoalAccessor) (Object) this;

        Mob mob = accessor.eroded$getMob();

        if (!(mob.level() instanceof ServerLevel world)) {
            return false;
        }

        BlockPos doorPos = accessor.eroded$getDoorPos();

        return doorPos != null
                && MobGriefingProtectionManager.isProtected(world, doorPos);
    }
}
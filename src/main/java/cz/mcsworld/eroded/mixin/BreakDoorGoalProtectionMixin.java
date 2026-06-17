package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.MobGriefingProtectionManager;
import net.minecraft.entity.ai.goal.BreakDoorGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalProtectionMixin {

    @Inject(
            method = "canStart",
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
            method = "shouldContinue",
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

        MobEntity mob = accessor.eroded$getMob();

        if (!(mob.getWorld() instanceof ServerWorld world)) {
            return false;
        }

        BlockPos doorPos = accessor.eroded$getDoorPos();

        return doorPos != null
                && MobGriefingProtectionManager.isProtected(world, doorPos);
    }
}
package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.world.darkness.DarknessEnvironment;
import cz.mcsworld.eroded.world.darkness.DarknessLightResolver;
import cz.mcsworld.eroded.world.darkness.DarknessMobLightMemory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCanTargetMixin {

    @Inject(method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void eroded$lightControlsAggression(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        var root = DarknessConfigs.get();
        if (!root.enabled) return;

        if (!root.server.mobLightFearEnabled) return;
        if (!((Object) this instanceof HostileEntity mob)) return;
        if (!(mob.getWorld() instanceof ServerWorld world) || !mob.isAlive()) return;

        if (mob.distanceTo(target) < 4.0f || mob.getAttacker() == target) return;

        BlockPos pos = mob.getBlockPos();
        if (!DarknessEnvironment.isDarkForMobs(world, pos)) return;

        if (DarknessMobLightMemory.isInPostLightPause(mob) ||
                DarknessLightResolver.isMobSuppressed(world, pos)) {
            cir.setReturnValue(false);
        }
    }
}
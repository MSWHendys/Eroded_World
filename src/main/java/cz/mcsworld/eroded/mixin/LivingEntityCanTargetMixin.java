package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.RespawnProtectionManager;
import cz.mcsworld.eroded.server.spawn.SpawnProtectionSystem;
import cz.mcsworld.eroded.world.darkness.DarknessEnvironment;
import cz.mcsworld.eroded.world.darkness.DarknessLightResolver;
import cz.mcsworld.eroded.world.darkness.DarknessMobLightMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCanTargetMixin {

    @Inject(
            method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$respawnProtectionPreventsTargeting(
            LivingEntity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (target instanceof ServerPlayer player
                && RespawnProtectionManager.isProtected(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$lightControlsAggression(
            LivingEntity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        var root = DarknessConfigs.get();

        if (!root.enabled) return;
        if (!root.server.mobLightFearEnabled) return;
        if (!((Object) this instanceof Monster mob)) return;
        if (!(mob.level() instanceof ServerLevel world) || !mob.isAlive()) return;

        if (mob.distanceTo(target) < 4.0f || mob.getLastHurtByMob() == target) return;

        BlockPos pos = mob.blockPosition();

        if (!DarknessEnvironment.isDarkForMobs(world, pos)) return;

        if (DarknessMobLightMemory.isInPostLightPause(mob)
                || DarknessLightResolver.isMobSuppressed(world, pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "hurtServer",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$respawnProtectionPreventsDamage(
            ServerLevel world,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Object self = this;

        if (!(self instanceof ServerPlayer player)) {
            return;
        }

        if (RespawnProtectionManager.shouldPreventDamage(player)
                || SpawnProtectionSystem.shouldPreventDamage(player)) {
            cir.setReturnValue(false);
        }
    }
}
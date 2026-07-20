package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.EntityProtectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityDamageMixin {

    @Inject(
            method = "hurtServer",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$protectBlockAttachedEntityDamage(
            ServerLevel world,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Entity target = (Entity) (Object) this;

        if (EntityProtectionEvents.canDamageProtectedEntity(
                target,
                world,
                source
        )) {
            cir.setReturnValue(false);
        }
    }
}
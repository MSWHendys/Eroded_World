package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.EntityProtectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityDamageMixin {

    @Inject(
            method = "damage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$protectBlockAttachedEntityDamage(
            ServerWorld world,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Entity target = (Entity) (Object) this;

        if (!EntityProtectionEvents.canDamageProtectedEntity(
                target,
                world,
                source
        )) {
            cir.setReturnValue(false);
        }
    }
}
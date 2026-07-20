package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.VehicleProtectionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleEntity.class)
public abstract class VehicleDamageProtectionMixin {

    @Inject(
            method = "hurtServer",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$protectVehicleDamageInClaim(
            ServerLevel world,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Entity vehicle = (Entity) (Object) this;

        if (!VehicleProtectionManager.canDamageVehicle(
                vehicle,
                world,
                source
        )) {
            cir.setReturnValue(false);
        }
    }
}
package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.VehicleProtectionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleEntity.class)
public abstract class VehicleDamageProtectionMixin {

    @Inject(
            method = "damage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$protectVehicleDamageInClaim(
            ServerWorld world,
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
package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.ProjectileProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHurtingProjectile.class)
public abstract class ExplosiveProjectileBoundaryMixin {

    @Unique
    private BlockPos eroded$previousBlockPos;

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void eroded$rememberProjectilePosition(CallbackInfo ci) {
        Entity projectile = (Entity) (Object) this;
        eroded$previousBlockPos = projectile.blockPosition();
    }

    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void eroded$preventProjectileCrossingProtectionBoundary(CallbackInfo ci) {
        Entity projectile = (Entity) (Object) this;

        if (!(projectile.level() instanceof ServerLevel world)) {
            return;
        }

        if (eroded$previousBlockPos == null) {
            return;
        }

        BlockPos currentPos = projectile.blockPosition();

        if (currentPos.equals(eroded$previousBlockPos)) {
            return;
        }

        if (!ProjectileProtectionManager.canProjectileMoveBetween(
                world,
                eroded$previousBlockPos,
                currentPos
        )) {
            projectile.discard();
        }
    }
}
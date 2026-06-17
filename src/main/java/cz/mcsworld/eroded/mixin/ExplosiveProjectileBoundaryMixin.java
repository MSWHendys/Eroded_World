package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.ProjectileProtectionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExplosiveProjectileEntity.class)
public abstract class ExplosiveProjectileBoundaryMixin {

    @Unique
    private BlockPos eroded$previousBlockPos;

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void eroded$rememberProjectilePosition(CallbackInfo ci) {
        Entity projectile = (Entity) (Object) this;
        eroded$previousBlockPos = projectile.getBlockPos();
    }

    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void eroded$preventProjectileCrossingProtectionBoundary(CallbackInfo ci) {
        Entity projectile = (Entity) (Object) this;

        if (!(projectile.getWorld() instanceof ServerWorld world)) {
            return;
        }

        if (eroded$previousBlockPos == null) {
            return;
        }

        BlockPos currentPos = projectile.getBlockPos();

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
package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.ProjectileProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmallFireball.class)
public abstract class SmallFireballProtectionMixin {

    @Inject(
            method = "onHitBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventSmallFireballIgnitingProtectedBlocks(
            BlockHitResult hitResult,
            CallbackInfo ci
    ) {
        Entity projectile = (Entity) (Object) this;

        if (!(projectile.level() instanceof ServerLevel world)) {
            return;
        }

        BlockPos hitPos = hitResult.getBlockPos();
        BlockPos firePos = hitPos.relative(hitResult.getDirection());

        if (!ProjectileProtectionManager.canProjectileAffect(world, projectile, hitPos)
                || !ProjectileProtectionManager.canProjectileAffect(world, projectile, firePos)) {
            projectile.discard();
            ci.cancel();
        }
    }
}
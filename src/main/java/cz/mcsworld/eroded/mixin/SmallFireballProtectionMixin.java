package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.ProjectileProtectionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmallFireballEntity.class)
public abstract class SmallFireballProtectionMixin {

    @Inject(
            method = "onBlockHit",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventSmallFireballIgnitingProtectedBlocks(
            BlockHitResult hitResult,
            CallbackInfo ci
    ) {
        Entity projectile = (Entity) (Object) this;

        if (!(projectile.getWorld() instanceof ServerWorld world)) {
            return;
        }

        BlockPos hitPos = hitResult.getBlockPos();
        BlockPos firePos = hitPos.offset(hitResult.getSide());

        if (!ProjectileProtectionManager.canProjectileAffect(world, projectile, hitPos)
                || !ProjectileProtectionManager.canProjectileAffect(world, projectile, firePos)) {
            projectile.discard();
            ci.cancel();
        }
    }
}
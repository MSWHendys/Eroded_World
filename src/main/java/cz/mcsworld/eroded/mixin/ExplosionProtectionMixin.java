package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.ExplosionImpl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ExplosionImpl.class)
public abstract class ExplosionProtectionMixin {

    @Shadow @Final
    private ServerWorld world;

    @Inject(method = "destroyBlocks", at = @At("HEAD"))
    private void eroded$protectBlocks(List<BlockPos> blocks, CallbackInfo ci) {

        blocks.removeIf(pos -> {

            if (ExplosionProtectionManager.isExplosionProtectionEnabled() &&
                    ExplosionProtectionManager.isProtected(world, pos)) {
                return true;
            }

            return ExplosionProtectionManager.isProtected(world, pos);
        });
    }

    @Inject(method = "createFire", at = @At("HEAD"))
    private void eroded$protectFire(List<BlockPos> blocks, CallbackInfo ci) {

        if (!ExplosionProtectionManager.isExplosionProtectionEnabled()) {
            return;
        }

        blocks.removeIf(pos -> ExplosionProtectionManager.isProtected(world, pos));
    }


}
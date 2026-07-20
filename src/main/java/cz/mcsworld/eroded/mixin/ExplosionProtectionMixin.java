package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;

@Mixin(ServerExplosion.class)
public abstract class ExplosionProtectionMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "interactWithBlocks", at = @At("HEAD"))
    private void eroded$protectBlocks(List<BlockPos> blocks, CallbackInfo ci) {
        blocks.removeIf(pos ->
                TerritoryProtectionManager.isExplosionProtected(level, pos)
                        || (
                        ExplosionProtectionManager.isExplosionProtectionEnabled()
                                && ExplosionProtectionManager.isProtected(level, pos)
                )
        );
    }

    @Inject(method = "createFire", at = @At("HEAD"))
    private void eroded$protectFire(List<BlockPos> blocks, CallbackInfo ci) {
        blocks.removeIf(pos ->
                TerritoryProtectionManager.isExplosionProtected(level, pos)
                        || (
                        ExplosionProtectionManager.isExplosionProtectionEnabled()
                                && ExplosionProtectionManager.isProtected(level, pos)
                )
        );
    }
}
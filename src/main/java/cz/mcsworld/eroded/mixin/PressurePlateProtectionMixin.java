package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.RedstoneProtectionManager;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractPressurePlateBlock.class)
public abstract class PressurePlateProtectionMixin {

    @Inject(
            method = "onEntityCollision",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$protectPressurePlate(
            BlockState state,
            World world,
            BlockPos pos,
            Entity entity,
            EntityCollisionHandler handler,
            CallbackInfo ci
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!RedstoneProtectionManager.canEntityTriggerPressurePlate(
                entity,
                serverWorld,
                pos
        )) {
            ci.cancel();
        }
    }
}
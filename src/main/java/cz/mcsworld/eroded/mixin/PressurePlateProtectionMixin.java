package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.RedstoneProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BasePressurePlateBlock.class)
public abstract class PressurePlateProtectionMixin {

    @Inject(
            method = "entityInside",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$protectPressurePlate(
            BlockState state,
            Level world,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier handler,
            CallbackInfo ci
    ) {
        if (!(world instanceof ServerLevel serverWorld)) {
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
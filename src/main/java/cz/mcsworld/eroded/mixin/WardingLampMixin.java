package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class WardingLampMixin {

    @Inject(
            method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$checkLampStealth(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof PlayerEntity player) {

            boolean holdingLamp = player.getMainHandStack().isOf(ErodedBlocks.WARDING_LANTERN.asItem()) ||
                    player.getOffHandStack().isOf(ErodedBlocks.WARDING_LANTERN.asItem());

            if (holdingLamp) {
                var world = player.getWorld();
                var pos = player.getBlockPos();

                int skyLight = world.getLightLevel(LightType.SKY, pos);
                boolean isUnderground = pos.getY() < 50 || !world.isSkyVisible(pos);
                boolean isDarkEnough = skyLight <= 7 && isUnderground;

                if (isDarkEnough && player.getAttacker() == null) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
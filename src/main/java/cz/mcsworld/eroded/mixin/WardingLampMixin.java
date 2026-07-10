package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class WardingLampMixin {

    @Inject(
            method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$checkLampStealth(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof Player player) {

            boolean holdingLamp = player.getMainHandItem().is(ErodedBlocks.WARDING_LANTERN.asItem()) ||
                    player.getOffhandItem().is(ErodedBlocks.WARDING_LANTERN.asItem());

            if (holdingLamp) {
                var world = player.level();
                var pos = player.blockPosition();

                int skyLight = world.getBrightness(LightLayer.SKY, pos);
                boolean isUnderground = pos.getY() < 50 || !world.canSeeSky(pos);
                boolean isDarkEnough = skyLight <= 7 && isUnderground;

                if (isDarkEnough && player.getLastHurtByMob() == null) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
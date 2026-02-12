package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.energy.EnergyFoodHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackFinishUsingMixin {

    @Inject(
            method = "finishUsing",
            at = @At("TAIL")
    )
    private void eroded$onFinishUsing(
            World world,
            LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!world.isClient && user instanceof PlayerEntity player) {
            EnergyFoodHandler.onEat(player, (ItemStack)(Object)this);
        }
    }
}

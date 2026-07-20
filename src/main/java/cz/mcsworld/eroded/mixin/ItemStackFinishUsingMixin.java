package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.energy.EnergyFoodHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackFinishUsingMixin {

    @Inject(
            method = "finishUsingItem",
            at = @At("TAIL")
    )
    private void eroded$onFinishUsing(
            Level world,
            LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (world.isClientSide) return;
        if (!(user instanceof Player player)) return;

        ItemStack stack = (ItemStack)(Object)this;

        if (stack.get(DataComponents.FOOD) == null) return;

        EnergyFoodHandler.onEat(player, stack);
    }
}

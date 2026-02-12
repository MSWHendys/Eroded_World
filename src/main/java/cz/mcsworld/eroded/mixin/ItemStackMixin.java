package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.energy.EnergyFoodHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(
            method = "finishUsing",
            at = @At("TAIL")
    )
    private void eroded$onFoodConsumed(
            World world,
            LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (world.isClient) return;
        if (!(user instanceof PlayerEntity player)) return;

        ItemStack self = (ItemStack) (Object) this;

        if (self.get(DataComponentTypes.FOOD) == null) return;

        EnergyFoodHandler.onEat(player, self);
    }
}

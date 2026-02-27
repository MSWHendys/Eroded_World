package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.crafting.CraftingService;
import cz.mcsworld.eroded.crafting.context.CraftingContext;
import cz.mcsworld.eroded.crafting.context.CraftingContextFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {

    @Inject(method = "onTakeItem", at = @At("HEAD"), cancellable = true)
    private void eroded$onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {

        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;

        CraftingResultSlot self = (CraftingResultSlot) (Object) this;
        RecipeInputInventory inputInv = ((CraftingResultSlotAccessor) self).eroded$getInput();

        Recipe<?> recipe = null;

        CraftingContext context = CraftingContextFactory.create(serverPlayer, recipe, inputInv);

        boolean success = CraftingService.process(context, stack);
        if (!success) {
            stack.setCount(0);
            ci.cancel();
        }
    }
}
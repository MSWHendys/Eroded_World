package cz.mcsworld.eroded.crafting.context;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;


public final class CraftingContextFactory {

    private CraftingContextFactory() {}

    public static CraftingContext create(
            ServerPlayerEntity player,
            Recipe<?> recipe,
            Inventory inventory
    ) {
        List<ItemStack> inputs = new ArrayList<>();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }

        return new CraftingContext(
                player,
                recipe,
                inputs,
                CraftingStationType.VANILLA
        );
    }
}

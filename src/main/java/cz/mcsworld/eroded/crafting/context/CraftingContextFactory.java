package cz.mcsworld.eroded.crafting.context;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;


public final class CraftingContextFactory {

    private CraftingContextFactory() {}

    public static CraftingContext create(
            ServerPlayer player,
            Recipe<?> recipe,
            Container inventory
    ) {
        List<ItemStack> inputs = new ArrayList<>();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
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

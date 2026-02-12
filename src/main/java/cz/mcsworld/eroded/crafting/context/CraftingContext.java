package cz.mcsworld.eroded.crafting.context;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class CraftingContext {

    private final ServerPlayerEntity player;
    private final Recipe<?> recipe;
    private final List<ItemStack> inputs;
    private final CraftingStationType station;

    public CraftingContext(
            ServerPlayerEntity player,
            Recipe<?> recipe,
            List<ItemStack> inputs,
            CraftingStationType station
    ) {
        this.player = player;
        this.recipe = recipe;
        this.inputs = inputs;
        this.station = station;
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public Recipe<?> getRecipe() {
        return recipe;
    }

    public List<ItemStack> getInputs() {
        return inputs;
    }

    public CraftingStationType getStation() {
        return station;
    }
}

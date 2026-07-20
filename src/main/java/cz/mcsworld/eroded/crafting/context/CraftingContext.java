package cz.mcsworld.eroded.crafting.context;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public final class CraftingContext {

    private final ServerPlayer player;
    private final Recipe<?> recipe;
    private final List<ItemStack> inputs;
    private final CraftingStationType station;

    public CraftingContext(
            ServerPlayer player,
            Recipe<?> recipe,
            List<ItemStack> inputs,
            CraftingStationType station
    ) {
        this.player = player;
        this.recipe = recipe;
        this.inputs = inputs;
        this.station = station;
    }

    public ServerPlayer getPlayer() {
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

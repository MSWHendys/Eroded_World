package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import java.util.List;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EnergyCostResolver {

    public static int getBaseCraftingCost(ItemStack resultStack) {
        return getCostFromMaterial(resultStack);
    }

    public static int getBaseCraftingCostFromInputs(List<ItemStack> inputs) {

        var root = CraftingConfig.get();
        var cfg = root.energy;

        int highestTierCost = cfg.minorCraftCost;

        for (ItemStack stack : inputs) {
            if (stack.isEmpty()) continue;
            int cost = getCostFromMaterial(stack);
            highestTierCost = Math.max(highestTierCost, cost);
        }

        return highestTierCost;
    }

    private static int getCostFromMaterial(ItemStack stack) {

        var root = CraftingConfig.get();
        var cfg = root.energy;

        if (stack.is(Items.NETHERITE_INGOT) || stack.is(Items.NETHERITE_SCRAP) || stack.is(Items.NETHERITE_BLOCK)
                || stack.getItem().toString().contains("netherite_")) {
            return cfg.netheriteCraftCost;
        }

        if (stack.is(Items.DIAMOND) || stack.is(Items.DIAMOND_BLOCK)
                || stack.getItem().toString().contains("diamond_")) {
            return cfg.diamondCraftCost;
        }

        if (stack.is(Items.IRON_INGOT) || stack.is(Items.IRON_NUGGET) || stack.is(Items.IRON_BLOCK)
                || stack.getItem().toString().contains("iron_")) {
            return cfg.ironCraftCost;
        }

        if (stack.is(ItemTags.STONE_TOOL_MATERIALS) || stack.is(Items.COBBLESTONE) || stack.is(Items.STONE)
                || stack.getItem().toString().contains("stone_")) {
            return cfg.stoneCraftCost;
        }

        if (stack.is(ItemTags.PLANKS) || stack.is(ItemTags.LOGS)
                || stack.getItem().toString().contains("wooden_")) {
            return cfg.woodCraftCost;
        }

        return cfg.minorCraftCost;
    }

}
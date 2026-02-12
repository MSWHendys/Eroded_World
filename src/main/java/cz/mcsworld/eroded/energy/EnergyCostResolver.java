package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

import java.util.List;

public class EnergyCostResolver {

    public static int getBaseCraftingCost(ItemStack resultStack) {
        return getCostFromMaterial(resultStack);
    }

    public static int getBaseCraftingCostFromInputs(List<ItemStack> inputs) {
        CraftingConfig cfg = CraftingConfig.get();
        int highestTierCost = cfg.minorCraftCost;

        for (ItemStack stack : inputs) {
            if (stack.isEmpty()) continue;
            int cost = getCostFromMaterial(stack);
            highestTierCost = Math.max(highestTierCost, cost);
        }

        return highestTierCost;
    }

    private static int getCostFromMaterial(ItemStack stack) {
        CraftingConfig cfg = CraftingConfig.get();

        if (stack.isOf(Items.NETHERITE_INGOT) || stack.isOf(Items.NETHERITE_SCRAP) || stack.isOf(Items.NETHERITE_BLOCK)
                || stack.getItem().toString().contains("netherite_")) {
            return cfg.netheriteCraftCost;
        }

        if (stack.isOf(Items.DIAMOND) || stack.isOf(Items.DIAMOND_BLOCK)
                || stack.getItem().toString().contains("diamond_")) {
            return cfg.diamondCraftCost;
        }

        if (stack.isOf(Items.IRON_INGOT) || stack.isOf(Items.IRON_NUGGET) || stack.isOf(Items.IRON_BLOCK)
                || stack.getItem().toString().contains("iron_")) {
            return cfg.ironCraftCost;
        }

        if (stack.isIn(ItemTags.STONE_TOOL_MATERIALS) || stack.isOf(Items.COBBLESTONE) || stack.isOf(Items.STONE)
                || stack.getItem().toString().contains("stone_")) {
            return cfg.stoneCraftCost;
        }

        if (stack.isIn(ItemTags.PLANKS) || stack.isIn(ItemTags.LOGS)
                || stack.getItem().toString().contains("wooden_")) {
            return cfg.woodCraftCost;
        }

        return cfg.minorCraftCost;
    }

}
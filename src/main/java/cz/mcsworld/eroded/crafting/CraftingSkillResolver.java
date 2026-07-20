package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.skills.SkillType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CraftingSkillResolver {

    public static SkillType resolve(ItemStack stack) {

        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();

        if (item == ErodedItems.ENERGY_DRINK || item == ErodedItems.ADRENALINE_SHOT) {
            return SkillType.SMELTING;
        }

        if (item == ErodedBlocks.WARDING_LANTERN.asItem()) {
            return SkillType.WOODWORKING;
        }

        if (path.contains("wood")
                || path.contains("oak")
                || path.contains("spruce")
                || path.contains("birch")
                || path.contains("jungle")
                || path.contains("acacia")
                || path.contains("dark_oak")
                || path.contains("bamboo")) {
            return SkillType.WOODWORKING;
        }

        if (path.contains("iron")
                || path.contains("gold")
                || path.contains("copper")
                || path.contains("netherite")) {
            return SkillType.SMELTING;
        }

        return SkillType.WOODWORKING;
    }
}

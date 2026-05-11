package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.skills.SkillType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class CraftingSkillResolver {

    public static SkillType resolve(ItemStack stack) {

        Item item = stack.getItem();
        Identifier id = Registries.ITEM.getId(item);
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

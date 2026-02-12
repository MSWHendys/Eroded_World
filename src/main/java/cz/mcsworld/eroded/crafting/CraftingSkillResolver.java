package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.skills.SkillType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Rozhoduje, který skill dostane CG podle craftěného předmětu
 */
public class CraftingSkillResolver {

    public static SkillType resolve(ItemStack stack) {

        Identifier id = Registries.ITEM.getId(stack.getItem());
        String path = id.getPath();

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

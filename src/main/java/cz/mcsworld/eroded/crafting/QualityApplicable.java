package cz.mcsworld.eroded.crafting;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class QualityApplicable {

    public static boolean isApplicable(ItemStack stack) {

        if (stack == null || stack.isEmpty()) return false;

        if (stack.getItem() instanceof BlockItem) return false;

        return stack.isDamageableItem();
    }
}

package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.core.ErodedComponents;
import net.minecraft.item.ItemStack;

public class ItemQuality {

    public static void set(ItemStack stack, Quality quality) {
        stack.set(ErodedComponents.QUALITY, quality);
    }

    public static Quality get(ItemStack stack) {
        return stack.getOrDefault(ErodedComponents.QUALITY, Quality.STANDARD);
    }
}

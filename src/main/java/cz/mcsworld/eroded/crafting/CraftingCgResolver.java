package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import net.minecraft.item.ItemStack;

public class CraftingCgResolver {

    public static float getBaseCg(ItemStack stack) {

        var root = CraftingConfig.get();
        var cfg = root.cg;

        if (stack.isDamageable()) {
            return cfg.baseCgDamageable;
        }

        return cfg.baseCgGeneric;
    }
}

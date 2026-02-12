package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

public class QualityEffectsApplier {

    public static void apply(ItemStack stack, Quality quality) {

        if (!stack.isDamageable()) return;

        CraftingConfig cfg = CraftingConfig.get();

        int vanillaMax = stack.getMaxDamage();
        float mult = switch (quality) {
            case POOR -> cfg.poorDurabilityMultiplier;
            case STANDARD -> cfg.standardDurabilityMultiplier;
            case EXCELLENT -> cfg.excellentDurabilityMultiplier;
        };

        int newMax = Math.max(1, Math.round(vanillaMax * mult));

        if (newMax == vanillaMax) return;

        int currentDamage = stack.getDamage();
        float ratio = (float) currentDamage / vanillaMax;
        int newDamage = Math.round(ratio * newMax);

        stack.set(DataComponentTypes.MAX_DAMAGE, newMax);
        stack.setDamage(newDamage);
    }
}

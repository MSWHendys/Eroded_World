package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;

public class QualityRepairModifier {

    public static float getRepairMultiplier(Quality quality) {

        CraftingConfig cfg = CraftingConfig.get();

        return switch (quality) {
            case POOR -> cfg.poorRepairMultiplier;
            case STANDARD -> cfg.standardRepairMultiplier;
            case EXCELLENT -> cfg.excellentRepairMultiplier;
        };
    }
}

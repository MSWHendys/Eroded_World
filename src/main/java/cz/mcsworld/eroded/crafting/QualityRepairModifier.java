package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;

public class QualityRepairModifier {

    public static float getRepairMultiplier(Quality quality) {

        var root = CraftingConfig.get();
        var cfg = root.quality;

        return switch (quality) {
            case POOR -> cfg.poorRepairMultiplier;
            case STANDARD -> cfg.standardRepairMultiplier;
            case EXCELLENT -> cfg.excellentRepairMultiplier;
        };
    }
}

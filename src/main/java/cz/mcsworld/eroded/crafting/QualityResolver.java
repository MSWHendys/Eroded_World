package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillType;

public class QualityResolver {

    public static Quality resolveQuality(
            SkillData data,
            float inputQualityAvg
    ) {
        var root = CraftingConfig.get();
        var cfg = root.quality;

        float woodworking = data.getCg(SkillType.WOODWORKING);
        float smelting = data.getCg(SkillType.SMELTING);

        float skillScore = woodworking + smelting;

        float effectiveScore =
                skillScore
                        + (inputQualityAvg - 1.0f) * cfg.inputQualityInfluence;

        if (inputQualityAvg < cfg.poorInputBlockExcellentBelow
                && effectiveScore >= cfg.qualityStandardToExcellent) {
            return Quality.STANDARD;
        }

        if (effectiveScore >= cfg.qualityStandardToExcellent) {
            return Quality.EXCELLENT;
        }

        if (effectiveScore >= cfg.qualityPoorToStandard) {
            return Quality.STANDARD;
        }

        return Quality.POOR;
    }
}

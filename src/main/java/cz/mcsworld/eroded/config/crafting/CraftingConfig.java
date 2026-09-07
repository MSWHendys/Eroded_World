package cz.mcsworld.eroded.config.crafting;

import cz.mcsworld.eroded.config.ConfigValidation;
import cz.mcsworld.eroded.config.ConfigValidationException;
import cz.mcsworld.eroded.config.ErodedConfig;
import cz.mcsworld.eroded.config.ErodedConfigs;

public class CraftingConfig implements ErodedConfig {

    public boolean enabled = true;

    public Energy energy = new Energy();

    public Quality quality = new Quality();

    public Cg cg = new Cg();

    public static CraftingConfig get() {
        return ErodedConfigs.CRAFTING;
    }

    @Override
    public void validatePostLoad() throws ConfigValidationException {
        ConfigValidation.notNull(energy, "crafting.energy");
        ConfigValidation.notNull(quality, "crafting.quality");
        ConfigValidation.notNull(cg, "crafting.cg");

        ConfigValidation.min(energy.minimumCraftCost, 0, "crafting.energy.minimumCraftCost");
        ConfigValidation.min(energy.minorCraftCost, 0, "crafting.energy.minorCraftCost");
        ConfigValidation.min(energy.woodCraftCost, 0, "crafting.energy.woodCraftCost");
        ConfigValidation.min(energy.stoneCraftCost, 0, "crafting.energy.stoneCraftCost");
        ConfigValidation.min(energy.ironCraftCost, 0, "crafting.energy.ironCraftCost");
        ConfigValidation.min(energy.diamondCraftCost, 0, "crafting.energy.diamondCraftCost");
        ConfigValidation.min(energy.netheriteCraftCost, 0, "crafting.energy.netheriteCraftCost");
        ConfigValidation.min(energy.simpleRecipeEnergyMultiplier, 0.0f, "crafting.energy.simpleRecipeEnergyMultiplier");
        ConfigValidation.min(energy.normalRecipeEnergyMultiplier, 0.0f, "crafting.energy.normalRecipeEnergyMultiplier");
        ConfigValidation.min(energy.complexRecipeEnergyMultiplier, 0.0f, "crafting.energy.complexRecipeEnergyMultiplier");
        ConfigValidation.min(energy.poorQualityEnergyMultiplier, 0.0f, "crafting.energy.poorQualityEnergyMultiplier");
        ConfigValidation.min(energy.standardQualityEnergyMultiplier, 0.0f, "crafting.energy.standardQualityEnergyMultiplier");
        ConfigValidation.min(energy.excellentQualityEnergyMultiplier, 0.0f, "crafting.energy.excellentQualityEnergyMultiplier");

        ConfigValidation.min(quality.qualityPoorToStandard, 0.0f, "crafting.quality.qualityPoorToStandard");
        ConfigValidation.require(quality.qualityStandardToExcellent >= quality.qualityPoorToStandard
                        && Float.isFinite(quality.qualityStandardToExcellent),
                "crafting.quality.qualityStandardToExcellent",
                "must be finite and >= qualityPoorToStandard");
        ConfigValidation.min(quality.inputQualityInfluence, 0.0f, "crafting.quality.inputQualityInfluence");
        ConfigValidation.range(quality.poorInputBlockExcellentBelow, 0.0f, 1.0f,
                "crafting.quality.poorInputBlockExcellentBelow");
        ConfigValidation.min(quality.poorDurabilityMultiplier, 0.0f, "crafting.quality.poorDurabilityMultiplier");
        ConfigValidation.min(quality.standardDurabilityMultiplier, 0.0f, "crafting.quality.standardDurabilityMultiplier");
        ConfigValidation.min(quality.excellentDurabilityMultiplier, 0.0f, "crafting.quality.excellentDurabilityMultiplier");
        ConfigValidation.min(quality.poorRepairMultiplier, 0.0f, "crafting.quality.poorRepairMultiplier");
        ConfigValidation.min(quality.standardRepairMultiplier, 0.0f, "crafting.quality.standardRepairMultiplier");
        ConfigValidation.min(quality.excellentRepairMultiplier, 0.0f, "crafting.quality.excellentRepairMultiplier");

        ConfigValidation.min(cg.baseCgDamageable, 0.0f, "crafting.cg.baseCgDamageable");
        ConfigValidation.min(cg.baseCgGeneric, 0.0f, "crafting.cg.baseCgGeneric");
        ConfigValidation.min(cg.simpleRecipeCgMultiplier, 0.0f, "crafting.cg.simpleRecipeCgMultiplier");
        ConfigValidation.min(cg.normalRecipeCgMultiplier, 0.0f, "crafting.cg.normalRecipeCgMultiplier");
        ConfigValidation.min(cg.complexRecipeCgMultiplier, 0.0f, "crafting.cg.complexRecipeCgMultiplier");
    }

    public static class Energy {

        public boolean enabled = true;

        public int minimumCraftCost = 1;

        public int minorCraftCost = 0;

        public int woodCraftCost = 1;

        public int stoneCraftCost = 2;

        public int ironCraftCost = 3;

        public int diamondCraftCost = 4;

        public int netheriteCraftCost = 5;

        public float simpleRecipeEnergyMultiplier = 0.80f;

        public float normalRecipeEnergyMultiplier = 1.00f;

        public float complexRecipeEnergyMultiplier = 1.30f;

        public float poorQualityEnergyMultiplier = 1.10f;

        public float standardQualityEnergyMultiplier = 1.00f;

        public float excellentQualityEnergyMultiplier = 0.90f;
    }

    public static class Quality {

        public boolean enabled = true;

        public float qualityPoorToStandard = 40.0f;

        public float qualityStandardToExcellent = 120.0f;

        public float inputQualityInfluence = 15.0f;

        public float poorInputBlockExcellentBelow = 0.5f;

        public float poorDurabilityMultiplier = 0.65f;

        public float standardDurabilityMultiplier = 1.0f;

        public float excellentDurabilityMultiplier = 1.4f;

        public float poorRepairMultiplier = 0.80f;

        public float standardRepairMultiplier = 1.0f;

        public float excellentRepairMultiplier = 1.15f;
    }

    public static class Cg {

        public boolean enabled = true;

        public float baseCgDamageable = 0.6f;

        public float baseCgGeneric = 0.1f;

        public float simpleRecipeCgMultiplier = 0.75f;

        public float normalRecipeCgMultiplier = 1.00f;

        public float complexRecipeCgMultiplier = 1.25f;
    }
}

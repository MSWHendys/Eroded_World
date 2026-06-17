package cz.mcsworld.eroded.config.crafting;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "ErodedWorld/crafting")
public class CraftingConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Gui.CollapsibleObject
    public Energy energy = new Energy();

    @ConfigEntry.Gui.CollapsibleObject
    public Quality quality = new Quality();

    @ConfigEntry.Gui.CollapsibleObject
    public Cg cg = new Cg();

    public static CraftingConfig get() {
        return AutoConfig
                .getConfigHolder(CraftingConfig.class)
                .getConfig();
    }

    public static class Energy {

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public int minimumCraftCost = 1;

        @ConfigEntry.Gui.Tooltip
        public int minorCraftCost = 0;

        @ConfigEntry.Gui.Tooltip
        public int woodCraftCost = 1;

        @ConfigEntry.Gui.Tooltip
        public int stoneCraftCost = 2;

        @ConfigEntry.Gui.Tooltip
        public int ironCraftCost = 3;

        @ConfigEntry.Gui.Tooltip
        public int diamondCraftCost = 4;

        @ConfigEntry.Gui.Tooltip
        public int netheriteCraftCost = 5;

        @ConfigEntry.Gui.Tooltip
        public float simpleRecipeEnergyMultiplier = 0.80f;

        @ConfigEntry.Gui.Tooltip
        public float normalRecipeEnergyMultiplier = 1.00f;

        @ConfigEntry.Gui.Tooltip
        public float complexRecipeEnergyMultiplier = 1.30f;

        @ConfigEntry.Gui.Tooltip
        public float poorQualityEnergyMultiplier = 1.10f;

        @ConfigEntry.Gui.Tooltip
        public float standardQualityEnergyMultiplier = 1.00f;

        @ConfigEntry.Gui.Tooltip
        public float excellentQualityEnergyMultiplier = 0.90f;
    }

    public static class Quality {

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public float qualityPoorToStandard = 40.0f;

        @ConfigEntry.Gui.Tooltip
        public float qualityStandardToExcellent = 120.0f;

        @ConfigEntry.Gui.Tooltip
        public float inputQualityInfluence = 15.0f;

        @ConfigEntry.Gui.Tooltip
        public float poorInputBlockExcellentBelow = 0.5f;

        @ConfigEntry.Gui.Tooltip
        public float poorDurabilityMultiplier = 0.65f;

        @ConfigEntry.Gui.Tooltip
        public float standardDurabilityMultiplier = 1.0f;

        @ConfigEntry.Gui.Tooltip
        public float excellentDurabilityMultiplier = 1.4f;

        @ConfigEntry.Gui.Tooltip
        public float poorRepairMultiplier = 0.80f;

        @ConfigEntry.Gui.Tooltip
        public float standardRepairMultiplier = 1.0f;

        @ConfigEntry.Gui.Tooltip
        public float excellentRepairMultiplier = 1.15f;
    }

    public static class Cg {

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public float baseCgDamageable = 0.6f;

        @ConfigEntry.Gui.Tooltip
        public float baseCgGeneric = 0.1f;

        @ConfigEntry.Gui.Tooltip
        public float simpleRecipeCgMultiplier = 0.75f;

        @ConfigEntry.Gui.Tooltip
        public float normalRecipeCgMultiplier = 1.00f;

        @ConfigEntry.Gui.Tooltip
        public float complexRecipeCgMultiplier = 1.25f;
    }
}
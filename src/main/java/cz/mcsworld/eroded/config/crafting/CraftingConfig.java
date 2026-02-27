package cz.mcsworld.eroded.config.crafting;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "ErodedWorld/crafting")
public class CraftingConfig implements ConfigData {

    public static CraftingConfig get() {
        return AutoConfig
                .getConfigHolder(CraftingConfig.class)
                .getConfig();
    }

    /* =========================================================
       GLOBAL
     ========================================================= */

    @ConfigEntry.Gui.Tooltip
    // Zapne / vypne celý crafting overhaul systém
    public boolean enabled = true;

    /* =========================================================
       ENERGY COST SYSTEM
     ========================================================= */

    @ConfigEntry.Gui.CollapsibleObject
    public Energy energy = new Energy();

    public static class Energy {

        @ConfigEntry.Gui.Tooltip
        // Zapne / vypne spotřebu energie při craftění
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        // Základní cena craftění dřevěných předmětů
        public int woodCraftCost = 1;

        @ConfigEntry.Gui.Tooltip
        // Základní cena craftění kamenných předmětů
        public int stoneCraftCost = 2;

        @ConfigEntry.Gui.Tooltip
        // Základní cena craftění železných předmětů
        public int ironCraftCost = 3;

        @ConfigEntry.Gui.Tooltip
        // Základní cena craftění diamantových předmětů
        public int diamondCraftCost = 4;

        @ConfigEntry.Gui.Tooltip
        // Základní cena craftění netheritových předmětů
        public int netheriteCraftCost = 5;

        @ConfigEntry.Gui.Tooltip
        // Cena craftění ostatních (nezařazených) předmětů
        public int minorCraftCost = 0;

        @ConfigEntry.Gui.Tooltip
        // Minimální možná cena craftění (po všech násobičích)
        public int minimumCraftCost = 1;

        @ConfigEntry.Gui.Tooltip
        // Násobič energie pro jednoduché recepty
        public float simpleRecipeEnergyMultiplier = 0.80f;

        @ConfigEntry.Gui.Tooltip
        // Násobič energie pro běžné recepty
        public float normalRecipeEnergyMultiplier = 1.00f;

        @ConfigEntry.Gui.Tooltip
        // Násobič energie pro složité recepty
        public float complexRecipeEnergyMultiplier = 1.30f;

        @ConfigEntry.Gui.Tooltip
        // Násobič energie při nízké kvalitě výsledku
        public float poorQualityEnergyMultiplier = 1.10f;

        @ConfigEntry.Gui.Tooltip
        // Násobič energie při standardní kvalitě
        public float standardQualityEnergyMultiplier = 1.00f;

        @ConfigEntry.Gui.Tooltip
        // Násobič energie při výborné kvalitě
        public float excellentQualityEnergyMultiplier = 0.90f;
    }

    /* =========================================================
       CG (CRAFTING SKILL GAIN)
     ========================================================= */

    @ConfigEntry.Gui.CollapsibleObject
    public Cg cg = new Cg();

    public static class Cg {

        @ConfigEntry.Gui.Tooltip
        // Zapne / vypne získávání Crafting skill bodů (CG)
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        // Základní CG za craftění předmětů s odolností (nástroje, zbraně)
        public float baseCgDamageable = 0.6f;

        @ConfigEntry.Gui.Tooltip
        // Základní CG za craftění běžných (nedamageable) předmětů
        public float baseCgGeneric = 0.1f;

        @ConfigEntry.Gui.Tooltip
        // Násobič CG pro jednoduché recepty
        public float simpleRecipeCgMultiplier = 0.75f;

        @ConfigEntry.Gui.Tooltip
        // Násobič CG pro běžné recepty
        public float normalRecipeCgMultiplier = 1.00f;

        @ConfigEntry.Gui.Tooltip
        // Násobič CG pro složité recepty
        public float complexRecipeCgMultiplier = 1.25f;
    }

    /* =========================================================
       QUALITY SYSTEM
     ========================================================= */

    @ConfigEntry.Gui.CollapsibleObject
    public Quality quality = new Quality();

    public static class Quality {

        @ConfigEntry.Gui.Tooltip
        // Zapne / vypne systém kvality předmětů
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        // Hodnota potřebná pro přechod z POOR na STANDARD kvalitu
        public float qualityPoorToStandard = 40.0f;

        @ConfigEntry.Gui.Tooltip
        // Hodnota potřebná pro přechod ze STANDARD na EXCELLENT kvalitu
        public float qualityStandardToExcellent = 120.0f;

        @ConfigEntry.Gui.Tooltip
        // Jak moc ovlivňuje kvalita vstupních surovin výslednou kvalitu
        public float inputQualityInfluence = 15.0f;

        @ConfigEntry.Gui.Tooltip
        // Pokud je průměrná kvalita vstupů nižší než tato hodnota,
        // nelze dosáhnout EXCELLENT kvality
        public float poorInputBlockExcellentBelow = 0.5f;

        @ConfigEntry.Gui.Tooltip
        // Násobič maximální odolnosti při POOR kvalitě
        public float poorDurabilityMultiplier = 0.65f;

        @ConfigEntry.Gui.Tooltip
        // Násobič maximální odolnosti při STANDARD kvalitě
        public float standardDurabilityMultiplier = 1.0f;

        @ConfigEntry.Gui.Tooltip
        // Násobič maximální odolnosti při EXCELLENT kvalitě
        public float excellentDurabilityMultiplier = 1.4f;

        @ConfigEntry.Gui.Tooltip
        // Násobič efektivity oprav při POOR kvalitě
        public float poorRepairMultiplier = 0.80f;

        @ConfigEntry.Gui.Tooltip
        // Násobič efektivity oprav při STANDARD kvalitě
        public float standardRepairMultiplier = 1.0f;

        @ConfigEntry.Gui.Tooltip
        // Násobič efektivity oprav při EXCELLENT kvalitě
        public float excellentRepairMultiplier = 1.15f;
    }
}
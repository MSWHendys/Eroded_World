package cz.mcsworld.eroded.config;

import cz.mcsworld.eroded.config.combat.CombatConfig;
import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.config.loot.LootConfig;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;

import me.shedaniel.autoconfig.AutoConfig;

public final class ErodedConfigs {

    private ErodedConfigs() {}
    public static LootConfig LOOT;
    public static void reload() {

        AutoConfig.getConfigHolder(CombatConfig.class).load();
        AutoConfig.getConfigHolder(CraftingConfig.class).load();
        AutoConfig.getConfigHolder(DarknessConfigs.class).load();
        AutoConfig.getConfigHolder(DeathConfig.class).load();
        AutoConfig.getConfigHolder(EnergyConfig.class).load();
        AutoConfig.getConfigHolder(TerritoryConfig.class).load();
        AutoConfig.getConfigHolder(LootConfig.class).load();
        LOOT = AutoConfig.getConfigHolder(LootConfig.class).getConfig();

    }
}
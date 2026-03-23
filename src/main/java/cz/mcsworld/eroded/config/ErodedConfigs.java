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

        reloadSingle(CombatConfig.class);
        reloadSingle(CraftingConfig.class);
        reloadSingle(DarknessConfigs.class);
        reloadSingle(DeathConfig.class);
        reloadSingle(EnergyConfig.class);
        reloadSingle(TerritoryConfig.class);
        reloadSingle(LootConfig.class);

        LOOT = AutoConfig.getConfigHolder(LootConfig.class).get();

        System.out.println("[Eroded] Konfigurace synchronizovány pro 1.21.6 - 1.21.8");
    }

    private static <T extends me.shedaniel.autoconfig.ConfigData> void reloadSingle(Class<T> clazz) {
        var holder = AutoConfig.getConfigHolder(clazz);
        holder.load();
    }
}
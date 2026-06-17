package cz.mcsworld.eroded.config;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.config.combat.CombatConfig;
import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.config.loot.LootConfig;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;

public final class ErodedConfigs {

    private ErodedConfigs() {
    }

    public static CombatConfig COMBAT;
    public static CraftingConfig CRAFTING;
    public static DarknessConfigs DARKNESS;
    public static DeathConfig DEATH;
    public static EnergyConfig ENERGY;
    public static TerritoryConfig TERRITORY;
    public static LootConfig LOOT;

    public static void reload() {
        COMBAT = reloadSingle(CombatConfig.class);
        CRAFTING = reloadSingle(CraftingConfig.class);
        DARKNESS = reloadSingle(DarknessConfigs.class);
        DEATH = reloadSingle(DeathConfig.class);
        ENERGY = reloadSingle(EnergyConfig.class);
        TERRITORY = reloadSingle(TerritoryConfig.class);

        LOOT = reloadLootConfig();

        ErodedMod.LOGGER.info("[Eroded World] Configuration files reloaded successfully.");
    }

    private static LootConfig reloadLootConfig() {
        var holder = AutoConfig.getConfigHolder(LootConfig.class);

        holder.load();

        LootConfig config = holder.get();

        if (config.ensureDefaultEntries()) {
            holder.save();

            ErodedMod.LOGGER.info(
                    "[Eroded World] Loot config updated with missing default entries."
            );
        }

        return config;
    }

    private static <T extends ConfigData> T reloadSingle(Class<T> clazz) {
        var holder = AutoConfig.getConfigHolder(clazz);

        holder.load();
        holder.save();

        return holder.get();
    }
}
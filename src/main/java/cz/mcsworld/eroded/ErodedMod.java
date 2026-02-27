package cz.mcsworld.eroded;

import cz.mcsworld.eroded.combat.DodgeHandler;
import cz.mcsworld.eroded.combat.SprintEnergyHandler;
import cz.mcsworld.eroded.command.ErodedCommand;
import cz.mcsworld.eroded.config.combat.CombatConfig;
import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.core.ErodedComponents;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.*;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.energy.EnergySleepHandler;
import cz.mcsworld.eroded.energy.EnergySyncHandler;
import cz.mcsworld.eroded.network.NetworkPayloads;
import cz.mcsworld.eroded.world.darkness.*;
import cz.mcsworld.eroded.world.loot.MutatedMobLootHandler;
import cz.mcsworld.eroded.world.spawn.SpawnProtectionTicker;
import cz.mcsworld.eroded.world.territory.TerritoryCaveCollapseHandler;
import cz.mcsworld.eroded.world.territory.TerritoryMobSpawnHandler;
import cz.mcsworld.eroded.world.territory.ecosystem.TerritoryEcosystemTicker;
import cz.mcsworld.eroded.world.territory.TerritoryMiningListener;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErodedMod implements ModInitializer {

    public static final String MOD_ID = "eroded";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        NetworkPayloads.registerAll();

        AutoConfig.register(EnergyConfig.class, GsonConfigSerializer::new);
        AutoConfig.register(CraftingConfig.class, GsonConfigSerializer::new);
        AutoConfig.register(DarknessConfigs.class, GsonConfigSerializer::new);
        AutoConfig.register(TerritoryConfig.class, GsonConfigSerializer::new);
        AutoConfig.register(DeathConfig.class, GsonConfigSerializer::new);
        AutoConfig.register(CombatConfig.class, GsonConfigSerializer::new);


        ErodedItems.register();
        ErodedBlocks.register();
        ErodedComponents.register();

        //SprintEnergyHandler.register();
        //DodgeHandler.register();
        CombatConfig combat = CombatConfig.get();

        if (combat.enabled) {

            if (combat.sprint.enabled) {
                SprintEnergyHandler.register();
            }

            if (combat.dodge.enabled) {
                DodgeHandler.register();
            }
        }

        EnergySyncHandler.register();
        DarknessChecker.register();
        DarknessMobAIInit.register();
        DarknessLightEater.register();
        MutatedMobHandler.register();
        TerritoryMiningListener.register();
        TerritoryMobSpawnHandler.register();
        TerritoryCaveCollapseHandler.register();
        TerritoryEcosystemTicker.register();
        MutatedMobLootHandler.register();
        ErodedCommand.register();
        DeathChestHandler.register();
        DeathChestAccessHandler.register();
        DeathChestBreakHandler.register();
        DeathCompassDropCleaner.register();
        DeathRespawnHandler.register();
        TraumaEffectHandler.register();

        DeathChestExpiryTicker.register();
        DeathChestParticles.register();
        DeathHologramHandler.register();

        DeathHologramOrphanCleaner.register();
        DeathHologramInteractBlocker.register();

        ErodedCompassServerTicker.register();
        EnergySleepHandler.register();
        SpawnProtectionTicker.register();

        LOGGER.info("[Eroded World] - mod initialized – death system stabilized.");
    }
}

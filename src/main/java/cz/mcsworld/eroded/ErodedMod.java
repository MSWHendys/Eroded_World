package cz.mcsworld.eroded;

import cz.mcsworld.eroded.combat.DodgeHandler;
import cz.mcsworld.eroded.combat.SprintEnergyHandler;
import cz.mcsworld.eroded.command.ErodedCommand;
import cz.mcsworld.eroded.config.ErodedConfigs;
import cz.mcsworld.eroded.config.combat.CombatConfig;
import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.config.loot.LootConfig;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.core.ErodedComponents;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.*;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.energy.EnergySleepHandler;
import cz.mcsworld.eroded.energy.EnergySyncHandler;
import cz.mcsworld.eroded.loot.ErodedContainerBreakHandler;
import cz.mcsworld.eroded.loot.ErodedContainerHandler;
import cz.mcsworld.eroded.loot.ErodedContainerPlacementHandler;
import cz.mcsworld.eroded.loot.ErodedContainerProtectionHandler;
import cz.mcsworld.eroded.network.NetworkPayloads;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.storage.SkillDataStorage;
import cz.mcsworld.eroded.world.darkness.*;
import cz.mcsworld.eroded.world.loot.MutatedMobLootHandler;
import cz.mcsworld.eroded.world.spawn.SpawnProtectionSpawnBlocker;
import cz.mcsworld.eroded.world.spawn.SpawnProtectionTicker;
import cz.mcsworld.eroded.world.territory.TerritoryCaveCollapseHandler;
import cz.mcsworld.eroded.world.territory.TerritoryMobSpawnHandler;
import cz.mcsworld.eroded.world.territory.ecosystem.TerritoryEcosystemTicker;
import cz.mcsworld.eroded.world.territory.TerritoryMiningListener;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

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
        AutoConfig.register(LootConfig.class, GsonConfigSerializer::new);
        ErodedConfigs.reload();


        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();

            SkillManager.save(handler.getPlayer());
            SkillManager.remove(uuid);

            DodgeHandler.cleanup(uuid);
            SprintEnergyHandler.cleanup(uuid);
            DarknessChecker.cleanup(uuid);
        });

        ErodedItems.register();
        ErodedBlocks.register();
        ErodedComponents.register();

        ErodedContainerHandler.register();
        ErodedContainerBreakHandler.register();
        ErodedContainerPlacementHandler.register();
        ErodedContainerProtectionHandler.register();

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
        SpawnProtectionSpawnBlocker.register();


        LOGGER.info("[Eroded World] - mod initialized – death system stabilized.");
    }
}

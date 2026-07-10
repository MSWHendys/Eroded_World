package cz.mcsworld.eroded;

import cz.mcsworld.eroded.block.ErodedBlockInteractionHandler;

import cz.mcsworld.eroded.combat.DodgeHandler;
import cz.mcsworld.eroded.combat.SprintEnergyHandler;
import cz.mcsworld.eroded.command.ErodedCommand;
import cz.mcsworld.eroded.config.*;
import cz.mcsworld.eroded.config.combat.CombatConfig;
import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.config.loot.LootConfig;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.core.*;
import cz.mcsworld.eroded.death.*;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.energy.EnergySleepHandler;
import cz.mcsworld.eroded.energy.EnergySyncHandler;
import cz.mcsworld.eroded.loot.ErodedContainerBreakHandler;
import cz.mcsworld.eroded.loot.ErodedContainerHandler;
import cz.mcsworld.eroded.loot.ErodedContainerPlacementHandler;
import cz.mcsworld.eroded.loot.ErodedContainerProtectionHandler;
import cz.mcsworld.eroded.network.NetworkPayloads;
import cz.mcsworld.eroded.network.TerritoryModuleNetworking;
import cz.mcsworld.eroded.network.TerritoryPlacementHintNetworking;
import cz.mcsworld.eroded.protection.EntityProtectionEvents;
import cz.mcsworld.eroded.protection.TerritoryProtectionEvents;
import cz.mcsworld.eroded.protection.VehicleProtectionEvents;
import cz.mcsworld.eroded.skills.SkillManager;

import cz.mcsworld.eroded.world.darkness.*;
import cz.mcsworld.eroded.world.loot.MutatedMobLootHandler;
import cz.mcsworld.eroded.world.spawn.SpawnProtectionSpawnBlocker;
import cz.mcsworld.eroded.world.spawn.SpawnProtectionTicker;
import cz.mcsworld.eroded.world.territory.*;
import cz.mcsworld.eroded.world.territory.ecosystem.TerritoryEcosystemTicker;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import java.util.UUID;

public class ErodedMod implements ModInitializer {

    public static final String MOD_ID = "eroded";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        NetworkPayloads.registerAll();
        TerritoryModuleNetworking.registerServerReceivers();

        AutoConfig.register(EnergyConfig.class, GsonConfigSerializer::new);
        AutoConfig.register(CraftingConfig.class, GsonConfigSerializer::new);
        AutoConfig.register(DarknessConfigs.class, GsonConfigSerializer::new);
        AutoConfig.register(TerritoryConfig.class, GsonConfigSerializer::new);
        AutoConfig.register(DeathConfig.class, GsonConfigSerializer::new);
        AutoConfig.register(CombatConfig.class, GsonConfigSerializer::new);
        AutoConfig.register(LootConfig.class, GsonConfigSerializer::new);
        ErodedEntities.register();

        FabricDefaultAttributeRegistry.register(
                ErodedEntities.ERODED_SPECIAL_SKELETON,
                AbstractSkeleton.createAttributes()
        );

        FabricDefaultAttributeRegistry.register(
                ErodedEntities.ERODED_SPECIAL_ZOMBIE,
                Zombie.createAttributes()
        );

        ErodedEntityItems.register();

        ErodedConfigs.reload();

        ErodedComponents.register();
        ErodedItems.register();
        ErodedBlocks.register();
        ErodedScreenHandlers.register();


        ErodedContainerHandler.register();
        ErodedContainerBreakHandler.register();
        ErodedContainerPlacementHandler.register();
        ErodedContainerProtectionHandler.register();

        SprintEnergyHandler.register();
        DodgeHandler.register();

        EnergySyncHandler.register();
        DarknessChecker.register();

        ErodedTorchHandler.register();
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
        RespawnProtectionManager.register();

        DeathChestExpiryTicker.register();
        DeathChestParticles.register();
        DeathHologramHandler.register();
        DeathCompassWeaponHandler.register();

        DeathHologramOrphanCleaner.register();
        DeathHologramInteractBlocker.register();

        ErodedCompassServerTicker.register();
        EnergySleepHandler.register();
        SpawnProtectionTicker.register();
        SpawnProtectionSpawnBlocker.register();

        ErodedLampHandler.register();
        ErodedBlockInteractionHandler.register();

        TerritoryProtectionEvents.register();
        EntityProtectionEvents.register();
        VehicleProtectionEvents.register();

        TerritoryPlacementHintNetworking.registerServerReceivers();


        LOGGER.info("[Eroded World] Mod initialized successfully.");

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUUID();

            var world = handler.getPlayer().level();

            SkillManager.save(handler.getPlayer());
            SkillManager.remove(uuid);

            DodgeHandler.cleanup(uuid);
            SprintEnergyHandler.cleanup(uuid);
            DarknessChecker.cleanup(uuid);

            ErodedLampHandler.cleanup(uuid, world);
            ErodedTorchHandler.cleanup(uuid, world);

        });
    }
}

package cz.mcsworld.eroded;

import cz.mcsworld.eroded.block.ErodedBlockInteractionHandler;

import cz.mcsworld.eroded.combat.DodgeHandler;
import cz.mcsworld.eroded.combat.SprintEnergyHandler;
import cz.mcsworld.eroded.command.ErodedCommand;
import cz.mcsworld.eroded.crafting.CraftingMessageCooldown;
import cz.mcsworld.eroded.config.*;
import cz.mcsworld.eroded.core.*;
import cz.mcsworld.eroded.death.*;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.energy.EnergySleepHandler;
import cz.mcsworld.eroded.energy.EnergySyncHandler;
import cz.mcsworld.eroded.energy.EnergyMovementHandler;
import cz.mcsworld.eroded.energy.MiningEnergyHandler;
import cz.mcsworld.eroded.loot.ErodedContainerBreakHandler;
import cz.mcsworld.eroded.loot.ErodedContainerHandler;
import cz.mcsworld.eroded.loot.ErodedContainerPlacementHandler;
import cz.mcsworld.eroded.loot.ErodedContainerProtectionHandler;
import cz.mcsworld.eroded.network.NetworkPayloads;
import cz.mcsworld.eroded.network.ServerPacketGuard;
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

        ErodedConfigs.initialize();

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
        EnergyMovementHandler.register();
        MiningEnergyHandler.register();
        DarknessChecker.register();

        DynamicLightManager.register();
        ErodedTorchHandler.register();
        DarknessMobAIInit.register();
        DarknessLightEater.register();
        MutatedMobHandler.register();
        TerritoryMiningListener.register();
        TerritoryMobSpawnHandler.register();
        TerritoryCaveCollapseHandler.register();
        TerritoryEcosystemTicker.register();
        TerritoryStateMaintenance.register();
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

        ErodedCompassHandler.register();
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

            SkillManager.persist(handler.getPlayer());
            SkillManager.remove(uuid);

            DodgeHandler.cleanup(uuid);
            SprintEnergyHandler.cleanup(uuid);
            DarknessChecker.cleanup(uuid);
            ServerPacketGuard.cleanup(uuid);
            ErodedCommand.cleanup(uuid);
            ErodedCompassItem.cleanup(uuid);
            ErodedCompassSyncHandler.cleanup(uuid);
            CraftingMessageCooldown.cleanup(uuid);

            ErodedLampHandler.cleanup(uuid, world);
            ErodedTorchHandler.cleanup(uuid, world);

        });
    }
}

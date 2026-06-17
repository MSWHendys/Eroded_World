package cz.mcsworld.eroded.death.block;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.block.*;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.item.ErodedTorchItem;
import cz.mcsworld.eroded.item.TerritoryAnchorItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class ErodedBlocks {

    public static Block DEATH_ENDER_CHEST;
    public static Block WARDING_LANTERN;
    public static Block ERODED_BLOCK;
    public static Block TERRITORY_ANCHOR;

    public static Block ERODED_TORCH;
    public static Block ERODED_WALL_TORCH;
    public static Item ERODED_TORCH_ITEM;

    public static void register() {

        Identifier chestId = Identifier.of(ErodedMod.MOD_ID, "death_ender_chest");
        RegistryKey<Block> chestBlockKey = RegistryKey.of(Registries.BLOCK.getKey(), chestId);
        RegistryKey<Item> chestItemKey = RegistryKey.of(Registries.ITEM.getKey(), chestId);

        DEATH_ENDER_CHEST = Registry.register(
                Registries.BLOCK,
                chestBlockKey,
                new DeathEnderChestBlock(
                        AbstractBlock.Settings.create()
                                .strength(50.0F, 1200.0F)
                                .requiresTool()
                                .registryKey(chestBlockKey)
                )
        );

        Registry.register(
                Registries.ITEM,
                chestItemKey,
                new BlockItem(
                        DEATH_ENDER_CHEST,
                        new Item.Settings()
                                .registryKey(chestItemKey)
                )
        );

        Identifier lanternId = Identifier.of(ErodedMod.MOD_ID, "eroded_lamp");
        RegistryKey<Block> lanternBlockKey = RegistryKey.of(Registries.BLOCK.getKey(), lanternId);
        RegistryKey<Item> lanternItemKey = RegistryKey.of(Registries.ITEM.getKey(), lanternId);

        WARDING_LANTERN = Registry.register(
                Registries.BLOCK,
                lanternBlockKey,
                new ErodedLampBlock(
                        AbstractBlock.Settings.create()
                                .luminance(state -> 15)
                                .strength(1.0F)
                                .registryKey(lanternBlockKey)
                )
        );

        Registry.register(
                Registries.ITEM,
                lanternItemKey,
                new BlockItem(
                        WARDING_LANTERN,
                        new Item.Settings()
                                .maxDamage(Math.max(
                                        1,
                                        DarknessConfigs.get()
                                                .server
                                                .wardingLamp
                                                .durationSeconds
                                ))
                                .registryKey(lanternItemKey)
                )
        );

        Identifier erodedBlockId = Identifier.of(ErodedMod.MOD_ID, "eroded_block");
        RegistryKey<Block> erodedBlockKey = RegistryKey.of(Registries.BLOCK.getKey(), erodedBlockId);
        RegistryKey<Item> erodedBlockItemKey = RegistryKey.of(Registries.ITEM.getKey(), erodedBlockId);

        ERODED_BLOCK = Registry.register(
                Registries.BLOCK,
                erodedBlockKey,
                new ErodedBlock(
                        AbstractBlock.Settings.create()
                                .strength(2.0F, 6.0F)
                                .registryKey(erodedBlockKey)
                )
        );

        Registry.register(
                Registries.ITEM,
                erodedBlockItemKey,
                new BlockItem(
                        ERODED_BLOCK,
                        new Item.Settings()
                                .registryKey(erodedBlockItemKey)
                )
        );

        Identifier territoryAnchorId = Identifier.of(ErodedMod.MOD_ID, "territory_anchor");

        RegistryKey<Block> territoryAnchorBlockKey =
                RegistryKey.of(Registries.BLOCK.getKey(), territoryAnchorId);

        RegistryKey<Item> territoryAnchorItemKey =
                RegistryKey.of(Registries.ITEM.getKey(), territoryAnchorId);

        TERRITORY_ANCHOR = Registry.register(
                Registries.BLOCK,
                territoryAnchorBlockKey,
                new TerritoryAnchorBlock(
                        AbstractBlock.Settings.create()
                                .strength(2.0F, 6.0F)
                                .nonOpaque()
                                .luminance(state -> state.get(TerritoryAnchorBlock.ACTIVE) ? 10 : 0)
                                .registryKey(territoryAnchorBlockKey)
                )
        );

        Registry.register(
                Registries.ITEM,
                territoryAnchorItemKey,
                new TerritoryAnchorItem(
                        TERRITORY_ANCHOR,
                        new Item.Settings()
                                .maxCount(16)
                                .registryKey(territoryAnchorItemKey)
                )
        );

        registerErodedTorch();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(ERODED_BLOCK.asItem()));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(WARDING_LANTERN.asItem());
            entries.add(DEATH_ENDER_CHEST.asItem());
            entries.add(TERRITORY_ANCHOR.asItem());
            entries.add(ERODED_TORCH_ITEM);
            entries.add(cz.mcsworld.eroded.core.ErodedItems.TERRITORY_MODULE);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(cz.mcsworld.eroded.core.ErodedItems.ENERGY_DRINK);
            entries.add(cz.mcsworld.eroded.core.ErodedItems.ADRENALINE_SHOT);
        });
    }


    private static void registerErodedTorch() {
        Identifier torchId =
                Identifier.of(ErodedMod.MOD_ID, "eroded_torch");

        Identifier wallTorchId =
                Identifier.of(ErodedMod.MOD_ID, "eroded_wall_torch");

        RegistryKey<Block> torchBlockKey = RegistryKey.of(
                Registries.BLOCK.getKey(),
                torchId
        );

        RegistryKey<Block> wallTorchBlockKey = RegistryKey.of(
                Registries.BLOCK.getKey(),
                wallTorchId
        );

        RegistryKey<Item> torchItemKey = RegistryKey.of(
                Registries.ITEM.getKey(),
                torchId
        );

        ERODED_TORCH = Registry.register(
                Registries.BLOCK,
                torchBlockKey,
                new ErodedTorchBlock(
                        AbstractBlock.Settings.create()
                                .noCollision()
                                .nonOpaque()
                                .strength(0.0F)
                                .luminance(state -> getConfiguredTorchLightLevel())
                                .registryKey(torchBlockKey)
                )
        );

        ERODED_WALL_TORCH = Registry.register(
                Registries.BLOCK,
                wallTorchBlockKey,
                new ErodedWallTorchBlock(
                        AbstractBlock.Settings.create()
                                .noCollision()
                                .nonOpaque()
                                .strength(0.0F)
                                .luminance(state -> getConfiguredTorchLightLevel())
                                .registryKey(wallTorchBlockKey)
                )
        );

        ERODED_TORCH_ITEM = Registry.register(
                Registries.ITEM,
                torchItemKey,
                new ErodedTorchItem(
                        ERODED_TORCH,
                        ERODED_WALL_TORCH,
                        new Item.Settings()
                                .maxDamage(Math.max(
                                        2,
                                        MathHelper.ceil(
                                                DarknessConfigs.get()
                                                        .server
                                                        .erodedTorch
                                                        .maxChargeTicks / 20.0F
                                        )
                                ))
                                .registryKey(torchItemKey)
                )
        );
    }

    private static int getConfiguredTorchLightLevel() {
        return MathHelper.clamp(
                DarknessConfigs.get()
                        .server
                        .erodedTorch
                        .placedLightLevel,
                0,
                15
        );
    }

    private ErodedBlocks() {
    }
}
package cz.mcsworld.eroded.death.block;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.block.*;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.item.ErodedTorchItem;
import cz.mcsworld.eroded.item.TerritoryAnchorItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ErodedBlocks {

    public static Block DEATH_ENDER_CHEST;
    public static Block WARDING_LANTERN;
    public static Block ERODED_BLOCK;
    public static Block TERRITORY_ANCHOR;

    public static Block ERODED_TORCH;
    public static Block ERODED_WALL_TORCH;
    public static Item ERODED_TORCH_ITEM;

    public static void register() {

        ResourceLocation chestId = ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "death_ender_chest");
        ResourceKey<Block> chestBlockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), chestId);
        ResourceKey<Item> chestItemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), chestId);

        DEATH_ENDER_CHEST = Registry.register(
                BuiltInRegistries.BLOCK,
                chestBlockKey,
                new DeathEnderChestBlock(
                        BlockBehaviour.Properties.of()
                                .strength(50.0F, 1200.0F)
                                .requiresCorrectToolForDrops()
                                .setId(chestBlockKey)
                )
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                chestItemKey,
                new BlockItem(
                        DEATH_ENDER_CHEST,
                        new Item.Properties()
                                .setId(chestItemKey)
                )
        );

        ResourceLocation lanternId = ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "eroded_lamp");
        ResourceKey<Block> lanternBlockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), lanternId);
        ResourceKey<Item> lanternItemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), lanternId);

        WARDING_LANTERN = Registry.register(
                BuiltInRegistries.BLOCK,
                lanternBlockKey,
                new ErodedLampBlock(
                        BlockBehaviour.Properties.of()
                                .lightLevel(state -> 15)
                                .strength(1.0F)
                                .setId(lanternBlockKey)
                )
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                lanternItemKey,
                new BlockItem(
                        WARDING_LANTERN,
                        new Item.Properties()
                                .durability(Math.max(
                                        1,
                                        DarknessConfigs.get()
                                                .server
                                                .wardingLamp
                                                .durationSeconds
                                ))
                                .setId(lanternItemKey)
                )
        );

        ResourceLocation erodedBlockId = ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "eroded_block");
        ResourceKey<Block> erodedBlockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), erodedBlockId);
        ResourceKey<Item> erodedBlockItemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), erodedBlockId);

        ERODED_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                erodedBlockKey,
                new ErodedBlock(
                        BlockBehaviour.Properties.of()
                                .strength(2.0F, 6.0F)
                                .setId(erodedBlockKey)
                )
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                erodedBlockItemKey,
                new BlockItem(
                        ERODED_BLOCK,
                        new Item.Properties()
                                .setId(erodedBlockItemKey)
                )
        );

        ResourceLocation territoryAnchorId = ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_anchor");

        ResourceKey<Block> territoryAnchorBlockKey =
                ResourceKey.create(BuiltInRegistries.BLOCK.key(), territoryAnchorId);

        ResourceKey<Item> territoryAnchorItemKey =
                ResourceKey.create(BuiltInRegistries.ITEM.key(), territoryAnchorId);

        TERRITORY_ANCHOR = Registry.register(
                BuiltInRegistries.BLOCK,
                territoryAnchorBlockKey,
                new TerritoryAnchorBlock(
                        BlockBehaviour.Properties.of()
                                .strength(2.0F, 6.0F)
                                .noOcclusion()
                                .lightLevel(state -> state.getValue(TerritoryAnchorBlock.ACTIVE) ? 10 : 0)
                                .setId(territoryAnchorBlockKey)
                )
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                territoryAnchorItemKey,
                new TerritoryAnchorItem(
                        TERRITORY_ANCHOR,
                        new Item.Properties()
                                .stacksTo(16)
                                .setId(territoryAnchorItemKey)
                )
        );

        registerErodedTorch();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> entries.accept(ERODED_BLOCK.asItem()));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(WARDING_LANTERN.asItem());
            entries.accept(DEATH_ENDER_CHEST.asItem());
            entries.accept(TERRITORY_ANCHOR.asItem());
            entries.accept(ERODED_TORCH_ITEM);
            entries.accept(cz.mcsworld.eroded.core.ErodedItems.TERRITORY_MODULE);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            entries.accept(cz.mcsworld.eroded.core.ErodedItems.ENERGY_DRINK);
            entries.accept(cz.mcsworld.eroded.core.ErodedItems.ADRENALINE_SHOT);
        });
    }


    private static void registerErodedTorch() {
        ResourceLocation torchId =
                ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "eroded_torch");

        ResourceLocation wallTorchId =
                ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "eroded_wall_torch");

        ResourceKey<Block> torchBlockKey = ResourceKey.create(
                BuiltInRegistries.BLOCK.key(),
                torchId
        );

        ResourceKey<Block> wallTorchBlockKey = ResourceKey.create(
                BuiltInRegistries.BLOCK.key(),
                wallTorchId
        );

        ResourceKey<Item> torchItemKey = ResourceKey.create(
                BuiltInRegistries.ITEM.key(),
                torchId
        );

        ERODED_TORCH = Registry.register(
                BuiltInRegistries.BLOCK,
                torchBlockKey,
                new ErodedTorchBlock(
                        BlockBehaviour.Properties.of()
                                .noCollission()
                                .noOcclusion()
                                .strength(0.0F)
                                .lightLevel(state -> getConfiguredTorchLightLevel())
                                .setId(torchBlockKey)
                )
        );

        ERODED_WALL_TORCH = Registry.register(
                BuiltInRegistries.BLOCK,
                wallTorchBlockKey,
                new ErodedWallTorchBlock(
                        BlockBehaviour.Properties.of()
                                .noCollission()
                                .noOcclusion()
                                .strength(0.0F)
                                .lightLevel(state -> getConfiguredTorchLightLevel())
                                .setId(wallTorchBlockKey)
                )
        );

        ERODED_TORCH_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                torchItemKey,
                new ErodedTorchItem(
                        ERODED_TORCH,
                        ERODED_WALL_TORCH,
                        new Item.Properties()
                                .durability(Math.max(
                                        2,
                                        Mth.ceil(
                                                DarknessConfigs.get()
                                                        .server
                                                        .erodedTorch
                                                        .maxChargeTicks / 20.0F
                                        )
                                ))
                                .setId(torchItemKey)
                )
        );
    }

    private static int getConfiguredTorchLightLevel() {
        return Mth.clamp(
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
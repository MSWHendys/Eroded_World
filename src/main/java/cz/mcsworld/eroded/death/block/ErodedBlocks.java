package cz.mcsworld.eroded.death.block;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.block.ErodedBlock;
import cz.mcsworld.eroded.block.ErodedLampBlock;
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
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;

public final class ErodedBlocks {

    public static Block DEATH_ENDER_CHEST;
    public static Block WARDING_LANTERN;
    public static Block ERODED_BLOCK;

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
                        new Item.Settings().registryKey(chestItemKey)
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
                                .strength(1.0f)
                                .registryKey(lanternBlockKey)
                )
        );

        Registry.register(
                Registries.ITEM,
                lanternItemKey,
                new BlockItem(
                        WARDING_LANTERN,
                        new Item.Settings()
                                .maxDamage(Math.max(1, DarknessConfigs.get().server.wardingLamp.durationSeconds))
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
                                .strength(2.0f, 6.0f)
                                .registryKey(erodedBlockKey)
                )
        );

        Registry.register(
                Registries.ITEM,
                erodedBlockItemKey,
                new BlockItem(
                        ERODED_BLOCK,
                        new Item.Settings().registryKey(erodedBlockItemKey)
                )
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(ERODED_BLOCK.asItem());
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(WARDING_LANTERN.asItem());
            entries.add(DEATH_ENDER_CHEST.asItem());
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(cz.mcsworld.eroded.core.ErodedItems.ENERGY_DRINK);
            entries.add(cz.mcsworld.eroded.core.ErodedItems.ADRENALINE_SHOT);
        });
    }

    private ErodedBlocks() {
    }
}
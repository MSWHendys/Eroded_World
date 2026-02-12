package cz.mcsworld.eroded.death.block;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public final class ErodedBlocks {

    public static Block DEATH_ENDER_CHEST;

    public static void register() {
        Identifier id = Identifier.of(ErodedMod.MOD_ID, "death_ender_chest");

        DEATH_ENDER_CHEST = Registry.register(
                Registries.BLOCK,
                id,
                new DeathEnderChestBlock(
                        AbstractBlock.Settings.create()
                                .strength(50.0F, 1200.0F)
                                .requiresTool()
                                .registryKey(
                                        RegistryKey.of(
                                                Registries.BLOCK.getKey(),
                                                id
                                        )
                                )
                )
        );

        Registry.register(
                Registries.ITEM,
                id,
                new BlockItem(
                        DEATH_ENDER_CHEST,
                        new Item.Settings()
                                .registryKey(
                                        RegistryKey.of(
                                                Registries.ITEM.getKey(),
                                                id
                                        )
                                )
                )
        );
    }

    private ErodedBlocks() {}
}

package cz.mcsworld.eroded.core;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.death.ErodedCompassItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public final class ErodedItems {

    public static final RegistryKey<Item> DEATH_COMPASS_KEY =
            RegistryKey.of(
                    Registries.ITEM.getKey(),
                    Identifier.of(ErodedMod.MOD_ID, "death_compass")
            );

    public static Item DEATH_COMPASS;

    public static void register() {
        DEATH_COMPASS = Registry.register(
                Registries.ITEM,
                DEATH_COMPASS_KEY.getValue(),
                new ErodedCompassItem(
                        new Item.Settings()
                                .maxCount(1)
                                .registryKey(DEATH_COMPASS_KEY)
                )
        );
    }

    private ErodedItems() {}
}

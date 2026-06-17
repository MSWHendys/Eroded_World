package cz.mcsworld.eroded.core;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.death.ErodedCompassItem;
import cz.mcsworld.eroded.item.AdrenalineShotItem;
import cz.mcsworld.eroded.item.EnergyDrinkItem;
import cz.mcsworld.eroded.item.TerritoryModuleItem;
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

    public static final RegistryKey<Item> ENERGY_DRINK_KEY =
            RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(ErodedMod.MOD_ID, "energy_drink"));

    public static Item ENERGY_DRINK;

    public static final RegistryKey<Item> ADRENALINE_SHOT_KEY =
            RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(ErodedMod.MOD_ID, "adrenaline_shot"));

    public static final Item ADRENALINE_SHOT = Registry.register(
            Registries.ITEM,
            ADRENALINE_SHOT_KEY.getValue(),
            new AdrenalineShotItem(new Item.Settings().maxCount(1).registryKey(ADRENALINE_SHOT_KEY))
    );

    public static final RegistryKey<Item> TERRITORY_MODULE_KEY =
            RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(ErodedMod.MOD_ID, "territory_module"));

    public static Item TERRITORY_MODULE;

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

        ENERGY_DRINK = Registry.register(
                Registries.ITEM,
                ENERGY_DRINK_KEY.getValue(),
                new EnergyDrinkItem(new Item.Settings().maxCount(16).registryKey(ENERGY_DRINK_KEY))
        );

        TERRITORY_MODULE = Registry.register(
                Registries.ITEM,
                TERRITORY_MODULE_KEY.getValue(),
                new TerritoryModuleItem(
                        new Item.Settings()
                                .maxCount(1)
                                .registryKey(TERRITORY_MODULE_KEY)
                )
        );
    }



    private ErodedItems() {}
}

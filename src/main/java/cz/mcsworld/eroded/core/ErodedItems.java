package cz.mcsworld.eroded.core;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.death.ErodedCompassItem;
import cz.mcsworld.eroded.item.AdrenalineShotItem;
import cz.mcsworld.eroded.item.EnergyDrinkItem;
import cz.mcsworld.eroded.item.TerritoryModuleItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public final class ErodedItems {

    public static final ResourceKey<@NotNull Item> DEATH_COMPASS_KEY =
            ResourceKey.create(
                    BuiltInRegistries.ITEM.key(),
                    Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "death_compass")
            );

    public static Item DEATH_COMPASS;

    public static final ResourceKey<@NotNull Item> ENERGY_DRINK_KEY =
            ResourceKey.create(BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "energy_drink"));

    public static Item ENERGY_DRINK;

    public static final ResourceKey<@NotNull Item> ADRENALINE_SHOT_KEY =
            ResourceKey.create(BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "adrenaline_shot"));

    public static final Item ADRENALINE_SHOT = Registry.register(
            BuiltInRegistries.ITEM,
            ADRENALINE_SHOT_KEY.identifier(),
            new AdrenalineShotItem(new Item.Properties().stacksTo(1).setId(ADRENALINE_SHOT_KEY))
    );

    public static final ResourceKey<@NotNull Item> TERRITORY_MODULE_KEY =
            ResourceKey.create(BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_module"));

    public static Item TERRITORY_MODULE;

    public static void register() {
        DEATH_COMPASS = Registry.register(
                BuiltInRegistries.ITEM,
                DEATH_COMPASS_KEY.identifier(),
                new ErodedCompassItem(
                        new Item.Properties()
                                .stacksTo(1)
                                .setId(DEATH_COMPASS_KEY)
                )
        );

        ENERGY_DRINK = Registry.register(
                BuiltInRegistries.ITEM,
                ENERGY_DRINK_KEY.identifier(),
                new EnergyDrinkItem(new Item.Properties().stacksTo(16).setId(ENERGY_DRINK_KEY))
        );

        TERRITORY_MODULE = Registry.register(
                BuiltInRegistries.ITEM,
                TERRITORY_MODULE_KEY.identifier(),
                new TerritoryModuleItem(
                        new Item.Properties()
                                .stacksTo(1)
                                .setId(TERRITORY_MODULE_KEY)
                )
        );
    }



    private ErodedItems() {}
}

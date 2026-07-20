package cz.mcsworld.eroded.core;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.screen.TerritoryModuleScreenData;
import cz.mcsworld.eroded.screen.TerritoryModuleScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ErodedScreenHandlers {

    public static final ExtendedScreenHandlerType<TerritoryModuleScreenHandler, TerritoryModuleScreenData> TERRITORY_MODULE =
            Registry.register(
                    BuiltInRegistries.MENU,
                    ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_module"),
                    new ExtendedScreenHandlerType<>(
                            TerritoryModuleScreenHandler::new,
                            TerritoryModuleScreenData.PACKET_CODEC
                    )
            );

    private ErodedScreenHandlers() {
    }

    public static void register() {

    }
}
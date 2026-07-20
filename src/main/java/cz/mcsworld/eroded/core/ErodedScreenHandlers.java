package cz.mcsworld.eroded.core;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.screen.TerritoryModuleScreenData;
import cz.mcsworld.eroded.screen.TerritoryModuleScreenHandler;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public final class ErodedScreenHandlers {

    public static final ExtendedMenuType<@NotNull TerritoryModuleScreenHandler, @NotNull TerritoryModuleScreenData> TERRITORY_MODULE =
            Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_module"),
                    new ExtendedMenuType<>(
                            TerritoryModuleScreenHandler::new,
                            TerritoryModuleScreenData.PACKET_CODEC
                    )
            );

    private ErodedScreenHandlers() {
    }

    public static void register() {

    }
}
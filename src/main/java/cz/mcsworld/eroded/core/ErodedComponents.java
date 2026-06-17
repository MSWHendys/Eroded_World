package cz.mcsworld.eroded.core;

import com.mojang.serialization.Codec;
import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.crafting.Quality;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ErodedComponents {

    public static final ComponentType<Quality> QUALITY =
            Registry.register(
                    Registries.DATA_COMPONENT_TYPE,
                    Identifier.of(ErodedMod.MOD_ID, "quality"),
                    ComponentType.<Quality>builder()
                            .codec(Codec.STRING.xmap(
                                    value -> {
                                        try {
                                            return Quality.valueOf(value);
                                        } catch (Exception exception) {
                                            return Quality.STANDARD;
                                        }
                                    },
                                    Quality::name
                            ))
                            .build()
            );


    private ErodedComponents() {
    }

    public static void register() {
        /* ----- */
    }
}
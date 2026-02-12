package cz.mcsworld.eroded.core;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.crafting.Quality;
import com.mojang.serialization.Codec;
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
                                    s -> {
                                        try {
                                            return Quality.valueOf(s);
                                        } catch (Exception e) {
                                            return Quality.STANDARD;
                                        }
                                    },
                                    Quality::name
                            ))
                            .build()
            );

    private ErodedComponents() {}

    public static void register() {
        // init hook
    }
}

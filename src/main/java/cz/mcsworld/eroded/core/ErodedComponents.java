package cz.mcsworld.eroded.core;

import com.mojang.serialization.Codec;
import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.crafting.Quality;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ErodedComponents {

    public static final DataComponentType<Quality> QUALITY =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "quality"),
                    DataComponentType.<Quality>builder()
                            .persistent(Codec.STRING.xmap(
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
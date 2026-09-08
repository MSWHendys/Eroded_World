package cz.mcsworld.eroded.client;

import com.mojang.blaze3d.platform.InputConstants;
import cz.mcsworld.eroded.client.debug.ErodedDebug;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;


public final class ErodedKeybinds {

    private static KeyMapping DEBUG_KEY;

    private static final KeyMapping.Category ERODED_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("eroded", "eroded"));

    public static void register() {

        DEBUG_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.eroded.debug",
                        InputConstants.KEY_F6,
                        ERODED_CATEGORY
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (DEBUG_KEY.consumeClick()) {
                ErodedDebug.territoryOverlay = !ErodedDebug.territoryOverlay;
            }

        });
    }
}
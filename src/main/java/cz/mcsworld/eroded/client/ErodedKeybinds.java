package cz.mcsworld.eroded.client;

import com.mojang.blaze3d.platform.InputConstants;
import cz.mcsworld.eroded.client.debug.ErodedDebug;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ErodedKeybinds {

    private static KeyMapping DEBUG_KEY;

    public static void register() {

        DEBUG_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.eroded.debug",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_F6,
                        "category.eroded"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (DEBUG_KEY.consumeClick()) {

                ErodedDebug.territoryOverlay = !ErodedDebug.territoryOverlay;

            }

        });
    }
}
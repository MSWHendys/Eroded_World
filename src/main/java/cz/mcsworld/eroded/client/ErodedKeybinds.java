package cz.mcsworld.eroded.client;

import cz.mcsworld.eroded.client.debug.ErodedDebug;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ErodedKeybinds {

    private static KeyBinding DEBUG_KEY;

    public static void register() {

        DEBUG_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.eroded.debug",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_F6,
                        "category.eroded"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (DEBUG_KEY.wasPressed()) {

                ErodedDebug.territoryOverlay = !ErodedDebug.territoryOverlay;

            }

        });
    }
}
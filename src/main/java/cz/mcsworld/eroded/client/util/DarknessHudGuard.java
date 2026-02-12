package cz.mcsworld.eroded.client.util;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import net.minecraft.client.MinecraftClient;

public final class DarknessHudGuard {

    private DarknessHudGuard() {}

    public static void enforceHudVisibility() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (DarknessClientData.isDarknessActive()) {

            if (client.options.hudHidden) {
                client.options.hudHidden = false;
            }
        }
    }
}

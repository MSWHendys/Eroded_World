package cz.mcsworld.eroded.client.util;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import net.minecraft.client.Minecraft;

public final class DarknessHudGuard {

    private DarknessHudGuard() {}

    public static void enforceHudVisibility() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (DarknessClientData.isDarknessActive()) {

            if (client.options.hideGui) {
                client.options.hideGui = false;
            }
        }
    }
}

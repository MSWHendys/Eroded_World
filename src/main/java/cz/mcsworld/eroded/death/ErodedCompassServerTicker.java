package cz.mcsworld.eroded.death;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ErodedCompassServerTicker {

    private ErodedCompassServerTicker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ErodedCompassServerTicker::tick);
    }

    private static void tick(MinecraftServer server) {
        // Compass targeting and inventory reconciliation do not need to scan
        // every player inventory 20 times per second.  Four times per second
        // keeps portal/dimension transitions responsive while cutting the
        // steady-state work by 80 %.
        if (server.getTickCount() % 5 != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ErodedCompassHandler.tick(player);
            ErodedCompassSyncHandler.sync(player);
        }
    }
}

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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ErodedCompassHandler.tick(player);
            ErodedCompassSyncHandler.sync(player);
        }
    }
}

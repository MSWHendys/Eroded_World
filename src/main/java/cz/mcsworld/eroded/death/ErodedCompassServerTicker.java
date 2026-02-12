package cz.mcsworld.eroded.death;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ErodedCompassServerTicker {

    private ErodedCompassServerTicker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ErodedCompassServerTicker::tick);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ErodedCompassHandler.tick(player);
            ErodedCompassSyncHandler.sync(player);
        }
    }
}

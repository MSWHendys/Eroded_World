package cz.mcsworld.eroded.network;


import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class SafeNetworkUtil {

    private SafeNetworkUtil() {}

    public static void safeSend(ServerPlayerEntity player, CustomPayload payload) {
        if (player == null) return;
        if (player.isDisconnected()) return;
        if (player.networkHandler == null) return;

        var server = player.getServer();
        if (server == null) return;

        if (server.isOnThread()) {

            ServerPlayNetworking.send(player, payload);
        } else {

            server.execute(() -> {
                if (!player.isDisconnected() && player.networkHandler != null) {
                    ServerPlayNetworking.send(player, payload);
                }
            });
        }
    }

    public static void safeDisconnect(
            ServerPlayerEntity player,
            Text reason
    ) {
        if (player == null) return;
        if (player.isDisconnected()) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        server.execute(() -> {
            if (!player.isDisconnected() && player.networkHandler != null) {
                player.networkHandler.disconnect(reason);
            }
        });
    }
}

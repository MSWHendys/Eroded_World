package cz.mcsworld.eroded.network;


import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

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

}

package cz.mcsworld.eroded.network;


import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class SafeNetworkUtil {

    private SafeNetworkUtil() {}

    public static void safeSend(ServerPlayer player, CustomPacketPayload payload) {
        if (player == null) return;
        if (player.hasDisconnected()) return;
        if (player.connection == null) return;

        var server = player.level().getServer();
        if (server == null) return;

        if (server.isSameThread()) {

            ServerPlayNetworking.send(player, payload);
        } else {

            server.execute(() -> {
                if (!player.hasDisconnected() && player.connection != null) {
                    ServerPlayNetworking.send(player, payload);
                }
            });
        }
    }

}

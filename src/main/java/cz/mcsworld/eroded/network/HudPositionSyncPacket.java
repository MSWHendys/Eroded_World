package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.config.energy.EnergyHudPosition;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends a player-local Energy HUD position from the server command to the client.
 *
 * <p>HUD placement is a client preference, so the dedicated server must not try
 * to persist it in its own energy.json. The receiving client applies and saves
 * the value locally.</p>
 */
public record HudPositionSyncPacket(EnergyHudPosition position)
        implements CustomPacketPayload {

    public static final Type<HudPositionSyncPacket> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("eroded", "hud_position"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HudPositionSyncPacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> buf.writeEnum(packet.position()),
                    buf -> new HudPositionSyncPacket(
                            buf.readEnum(EnergyHudPosition.class)
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void sendTo(ServerPlayer player, EnergyHudPosition position) {
        ServerPlayNetworking.send(player, new HudPositionSyncPacket(position));
    }
}

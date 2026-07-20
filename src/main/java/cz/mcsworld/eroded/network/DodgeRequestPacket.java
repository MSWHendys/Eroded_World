package cz.mcsworld.eroded.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DodgeRequestPacket(float dirX, float dirZ)
        implements CustomPacketPayload {

    public static final Type<DodgeRequestPacket> ID =
            new Type<>(Identifier.fromNamespaceAndPath("eroded", "dodge_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DodgeRequestPacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> {
                        buf.writeFloat(packet.dirX());
                        buf.writeFloat(packet.dirZ());
                    },
                    buf -> new DodgeRequestPacket(
                            buf.readFloat(),
                            buf.readFloat()
                    )
            );

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

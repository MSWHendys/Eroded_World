package cz.mcsworld.eroded.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DodgeRequestPacket(float dirX, float dirZ)
        implements CustomPayload {

    public static final Id<DodgeRequestPacket> ID =
            new Id<>(Identifier.of("eroded", "dodge_request"));

    public static final PacketCodec<RegistryByteBuf, DodgeRequestPacket> CODEC =
            PacketCodec.of(
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
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

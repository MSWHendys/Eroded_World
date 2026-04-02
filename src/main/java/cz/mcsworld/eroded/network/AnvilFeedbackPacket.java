package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AnvilFeedbackPacket(String key, String quality)
        implements CustomPayload {

    public static final Id<AnvilFeedbackPacket> ID =
            new Id<>(Identifier.of("eroded", "anvil_feedback"));

    public static final PacketCodec<RegistryByteBuf, AnvilFeedbackPacket> CODEC =
            PacketCodec.of(
                    (p, buf) -> {
                        buf.writeString(p.key());
                        buf.writeString(p.quality());
                    },
                    buf -> new AnvilFeedbackPacket(
                            buf.readString(),
                            buf.readString()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
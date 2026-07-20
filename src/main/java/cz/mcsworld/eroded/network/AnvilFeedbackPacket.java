package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AnvilFeedbackPacket(String key, String quality)
        implements CustomPacketPayload {

    public static final Type<AnvilFeedbackPacket> ID =
            new Type<>(Identifier.fromNamespaceAndPath("eroded", "anvil_feedback"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilFeedbackPacket> CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> {
                        buf.writeUtf(p.key());
                        buf.writeUtf(p.quality());
                    },
                    buf -> new AnvilFeedbackPacket(
                            buf.readUtf(),
                            buf.readUtf()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
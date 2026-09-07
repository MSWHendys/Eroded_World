package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record AnvilFeedbackPacket(String key, String quality)
        implements CustomPacketPayload {

    public static final int MAX_KEY_LENGTH = 128;
    public static final int MAX_QUALITY_LENGTH = 32;

    public static final Type<@NotNull AnvilFeedbackPacket> ID =
            new Type<>(Identifier.fromNamespaceAndPath("eroded", "anvil_feedback"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull AnvilFeedbackPacket> CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> {
                        buf.writeUtf(p.key(), MAX_KEY_LENGTH);
                        buf.writeUtf(p.quality(), MAX_QUALITY_LENGTH);
                    },
                    buf -> new AnvilFeedbackPacket(
                            buf.readUtf(MAX_KEY_LENGTH),
                            buf.readUtf(MAX_QUALITY_LENGTH)
                    )
            );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
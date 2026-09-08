package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record CompassDarknessBreakPacket(
        int durationTicks,
        float maxDarkness
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<@NotNull CompassDarknessBreakPacket> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("eroded", "compass_darkness_break"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull CompassDarknessBreakPacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> {
                        buf.writeVarInt(packet.durationTicks());
                        buf.writeFloat(packet.maxDarkness());
                    },
                    buf -> new CompassDarknessBreakPacket(
                            buf.readVarInt(),
                            buf.readFloat()
                    )
            );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
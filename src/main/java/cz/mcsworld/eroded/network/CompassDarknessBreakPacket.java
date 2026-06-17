package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CompassDarknessBreakPacket(
        int durationTicks,
        float maxDarkness
) implements CustomPayload {

    public static final CustomPayload.Id<CompassDarknessBreakPacket> ID =
            new CustomPayload.Id<>(Identifier.of("eroded", "compass_darkness_break"));

    public static final PacketCodec<RegistryByteBuf, CompassDarknessBreakPacket> CODEC =
            PacketCodec.of(
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
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
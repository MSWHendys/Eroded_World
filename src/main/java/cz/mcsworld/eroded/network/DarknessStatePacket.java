package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DarknessStatePacket(boolean inDarkness)
        implements CustomPayload {

    public static final CustomPayload.Id<DarknessStatePacket> ID =
            new CustomPayload.Id<>(Identifier.of("eroded", "darkness_state"));

    public static final PacketCodec<RegistryByteBuf, DarknessStatePacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> buf.writeBoolean(packet.inDarkness()),
                    buf -> new DarknessStatePacket(buf.readBoolean())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

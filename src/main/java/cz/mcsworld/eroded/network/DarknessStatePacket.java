package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DarknessStatePacket(boolean inDarkness)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DarknessStatePacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("eroded", "darkness_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DarknessStatePacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> buf.writeBoolean(packet.inDarkness()),
                    buf -> new DarknessStatePacket(buf.readBoolean())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

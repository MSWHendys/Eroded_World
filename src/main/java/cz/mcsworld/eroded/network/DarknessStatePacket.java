package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record DarknessStatePacket(boolean inDarkness)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<@NotNull DarknessStatePacket> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("eroded", "darkness_state"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull DarknessStatePacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> buf.writeBoolean(packet.inDarkness()),
                    buf -> new DarknessStatePacket(buf.readBoolean())
            );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}

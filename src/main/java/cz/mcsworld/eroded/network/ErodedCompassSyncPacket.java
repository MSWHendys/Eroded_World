package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ErodedCompassSyncPacket(
        boolean active,
        long remainingTicks,
        long deathPosLong
) implements CustomPacketPayload {

    public static final Type<ErodedCompassSyncPacket> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("eroded", "compass_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ErodedCompassSyncPacket> CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> {
                        buf.writeBoolean(p.active());
                        buf.writeLong(p.remainingTicks());
                        buf.writeLong(p.deathPosLong());
                    },
                    buf -> new ErodedCompassSyncPacket(
                            buf.readBoolean(),
                            buf.readLong(),
                            buf.readLong()
                    )
            );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ErodedCompassSyncPacket(
        boolean active,
        long remainingTicks,
        long deathPosLong
) implements CustomPayload {

    public static final Id<ErodedCompassSyncPacket> ID =
            new Id<>(Identifier.of("eroded", "compass_sync"));

    public static final PacketCodec<RegistryByteBuf, ErodedCompassSyncPacket> CODEC =
            PacketCodec.of(
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
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

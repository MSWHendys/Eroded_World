package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EnergySyncPacket(int energy) implements CustomPayload {

    public static final Id<EnergySyncPacket> ID =
            new Id<>(Identifier.of("eroded", "energy_sync"));

    public static final PacketCodec<RegistryByteBuf, EnergySyncPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT,
                    EnergySyncPacket::energy,
                    EnergySyncPacket::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

package cz.mcsworld.eroded.network;


import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EnergyWarningPacket() implements CustomPayload {

    public static final Id<EnergyWarningPacket> ID =
            new Id<>(Identifier.of("eroded", "energy_warning"));

    public static final PacketCodec<RegistryByteBuf, EnergyWarningPacket> CODEC =
            PacketCodec.unit(new EnergyWarningPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}



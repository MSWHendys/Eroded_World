package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.skills.SkillData;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EnergyWarningPacket(SkillData.EnergyState state) implements CustomPayload {

    public static final Id<EnergyWarningPacket> ID =
            new Id<>(Identifier.of("eroded", "energy_warning"));

    public static final PacketCodec<RegistryByteBuf, EnergyWarningPacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> buf.writeEnumConstant(packet.state()),
                    buf -> new EnergyWarningPacket(
                            buf.readEnumConstant(SkillData.EnergyState.class)
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
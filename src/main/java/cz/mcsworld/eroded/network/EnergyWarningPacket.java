package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.skills.SkillData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EnergyWarningPacket(SkillData.EnergyState state) implements CustomPacketPayload {

    public static final Type<EnergyWarningPacket> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("eroded", "energy_warning"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnergyWarningPacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> buf.writeEnum(packet.state()),
                    buf -> new EnergyWarningPacket(
                            buf.readEnum(SkillData.EnergyState.class)
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
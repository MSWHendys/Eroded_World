package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EnergySyncPacket(int energy, int maxEnergy, int immunitySeconds) implements CustomPacketPayload {

    public static final Type<EnergySyncPacket> ID =
            new Type<>(Identifier.fromNamespaceAndPath("eroded", "energy_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnergySyncPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, EnergySyncPacket::energy,
                    ByteBufCodecs.VAR_INT, EnergySyncPacket::maxEnergy,
                    ByteBufCodecs.VAR_INT, EnergySyncPacket::immunitySeconds,
                    EnergySyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
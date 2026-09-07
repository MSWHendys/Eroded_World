package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EnergySyncPacket(
        boolean enabled,
        int energy,
        int maxEnergy,
        int immunitySeconds,
        boolean miningEnabled,
        boolean miningSpeedScalingEnabled,
        boolean miningAllowAtZero,
        int miningFullSpeedFromPercent,
        int miningReducedSpeedFromPercent,
        int miningReducedSpeedPercent,
        int miningCriticalSpeedPercent
) implements CustomPacketPayload {

    public static final Type<EnergySyncPacket> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("eroded", "energy_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnergySyncPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, EnergySyncPacket::enabled,
                    ByteBufCodecs.VAR_INT, EnergySyncPacket::energy,
                    ByteBufCodecs.VAR_INT, EnergySyncPacket::maxEnergy,
                    ByteBufCodecs.VAR_INT, EnergySyncPacket::immunitySeconds,
                    ByteBufCodecs.BOOL, EnergySyncPacket::miningEnabled,
                    ByteBufCodecs.BOOL, EnergySyncPacket::miningSpeedScalingEnabled,
                    ByteBufCodecs.BOOL, EnergySyncPacket::miningAllowAtZero,
                    ByteBufCodecs.VAR_INT, EnergySyncPacket::miningFullSpeedFromPercent,
                    ByteBufCodecs.VAR_INT, EnergySyncPacket::miningReducedSpeedFromPercent,
                    ByteBufCodecs.VAR_INT, EnergySyncPacket::miningReducedSpeedPercent,
                    ByteBufCodecs.VAR_INT, EnergySyncPacket::miningCriticalSpeedPercent,
                    EnergySyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

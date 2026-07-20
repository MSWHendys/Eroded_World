package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkillSyncPacket(float woodworking, float smelting) implements CustomPacketPayload {

    public static final Type<SkillSyncPacket> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("eroded", "skill_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillSyncPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    SkillSyncPacket::woodworking,
                    ByteBufCodecs.FLOAT,
                    SkillSyncPacket::smelting,
                    SkillSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
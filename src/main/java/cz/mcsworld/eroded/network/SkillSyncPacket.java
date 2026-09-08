package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record SkillSyncPacket(float woodworking, float smelting) implements CustomPacketPayload {

    public static final Type<@NotNull SkillSyncPacket> ID =
            new Type<>(Identifier.fromNamespaceAndPath("eroded", "skill_sync"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull SkillSyncPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    SkillSyncPacket::woodworking,
                    ByteBufCodecs.FLOAT,
                    SkillSyncPacket::smelting,
                    SkillSyncPacket::new
            );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
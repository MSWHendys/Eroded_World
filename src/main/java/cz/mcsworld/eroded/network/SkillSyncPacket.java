package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SkillSyncPacket(float woodworking, float smelting) implements CustomPayload {

    public static final Id<SkillSyncPacket> ID =
            new Id<>(Identifier.of("eroded", "skill_sync"));

    public static final PacketCodec<RegistryByteBuf, SkillSyncPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.FLOAT,
                    SkillSyncPacket::woodworking,
                    PacketCodecs.FLOAT,
                    SkillSyncPacket::smelting,
                    SkillSyncPacket::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
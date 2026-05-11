package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CraftingRequirementPacket(String key) implements CustomPayload {

    public static final Id<CraftingRequirementPacket> ID =
            new Id<>(Identifier.of("eroded", "crafting_requirement"));

    public static final PacketCodec<RegistryByteBuf, CraftingRequirementPacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> buf.writeString(packet.key()),
                    buf -> new CraftingRequirementPacket(buf.readString())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CraftingFailPacket() implements CustomPayload {

    public static final Id<CraftingFailPacket> ID =
            new Id<>(Identifier.of("eroded", "crafting_fail"));

    public static final PacketCodec<RegistryByteBuf, CraftingFailPacket> CODEC =
            PacketCodec.unit(new CraftingFailPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

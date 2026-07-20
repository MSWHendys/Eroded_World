package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CraftingFailPacket() implements CustomPacketPayload {

    public static final Type<CraftingFailPacket> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("eroded", "crafting_fail"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingFailPacket> CODEC =
            StreamCodec.unit(new CraftingFailPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

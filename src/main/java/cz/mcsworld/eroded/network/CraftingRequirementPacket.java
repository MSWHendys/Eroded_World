package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CraftingRequirementPacket(String key) implements CustomPacketPayload {

    public static final int MAX_KEY_LENGTH = 128;

    public static final Type<CraftingRequirementPacket> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("eroded", "crafting_requirement"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingRequirementPacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> buf.writeUtf(packet.key(), MAX_KEY_LENGTH),
                    buf -> new CraftingRequirementPacket(buf.readUtf(MAX_KEY_LENGTH))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record CraftingRequirementPacket(String key) implements CustomPacketPayload {

    public static final int MAX_KEY_LENGTH = 128;

    public static final Type<@NotNull CraftingRequirementPacket> ID =
            new Type<>(Identifier.fromNamespaceAndPath("eroded", "crafting_requirement"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull CraftingRequirementPacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> buf.writeUtf(packet.key(), MAX_KEY_LENGTH),
                    buf -> new CraftingRequirementPacket(buf.readUtf(MAX_KEY_LENGTH))
            );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
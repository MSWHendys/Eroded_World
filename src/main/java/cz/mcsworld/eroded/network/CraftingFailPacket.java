package cz.mcsworld.eroded.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record CraftingFailPacket() implements CustomPacketPayload {

    public static final Type<@NotNull CraftingFailPacket> ID =
            new Type<>(Identifier.fromNamespaceAndPath("eroded", "crafting_fail"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull CraftingFailPacket> CODEC =
            StreamCodec.unit(new CraftingFailPacket());

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}

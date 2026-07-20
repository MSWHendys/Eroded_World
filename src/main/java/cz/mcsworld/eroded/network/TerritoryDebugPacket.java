package cz.mcsworld.eroded.network;


import org.jetbrains.annotations.NotNull;

public record TerritoryDebugPacket(
        int miningBlocks,
        int mining,
        int pollution,
        int forest,
        float threat
) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final Type<@NotNull TerritoryDebugPacket> ID =
            new Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("eroded", "territory_debug"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.@NotNull RegistryFriendlyByteBuf, @NotNull TerritoryDebugPacket> CODEC =
            net.minecraft.network.codec.StreamCodec.ofMember(
                    (p, buf) -> {
                        buf.writeInt(p.miningBlocks());
                        buf.writeInt(p.mining());
                        buf.writeInt(p.pollution());
                        buf.writeInt(p.forest());
                        buf.writeFloat(p.threat());
                    },
                    buf -> new TerritoryDebugPacket(
                            buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readFloat()
                    )
            );

    @Override
    public @NotNull Type<? extends net.minecraft.network.protocol.common.custom.@NotNull CustomPacketPayload> type() { return ID; }
}
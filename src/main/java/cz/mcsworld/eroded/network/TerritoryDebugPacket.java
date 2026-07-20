package cz.mcsworld.eroded.network;


public record TerritoryDebugPacket(
        int miningBlocks,
        int mining,
        int pollution,
        int forest,
        float threat
) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final Type<TerritoryDebugPacket> ID =
            new Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("eroded", "territory_debug"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, TerritoryDebugPacket> CODEC =
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
    public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return ID; }
}
package cz.mcsworld.eroded.network;

public record TerritoryDebugPacket(
        int miningBlocks,
        int mining,
        int pollution,
        int forest,
        float threat
) implements net.minecraft.network.packet.CustomPayload {

    public static final Id<TerritoryDebugPacket> ID =
            new Id<>(net.minecraft.util.Identifier.of("eroded", "territory_debug"));

    public static final net.minecraft.network.codec.PacketCodec<net.minecraft.network.RegistryByteBuf, TerritoryDebugPacket> CODEC =
            net.minecraft.network.codec.PacketCodec.of(
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
    public Id<? extends net.minecraft.network.packet.CustomPayload> getId() { return ID; }
}
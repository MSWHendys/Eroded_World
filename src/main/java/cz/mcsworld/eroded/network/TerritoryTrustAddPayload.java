package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record TerritoryTrustAddPayload(
        BlockPos anchorPos,
        String playerName,
        boolean connectedArea
) implements CustomPayload {

    public static final Id<TerritoryTrustAddPayload> ID =
            new Id<>(Identifier.of(ErodedMod.MOD_ID, "territory_trust_add"));

    public static final PacketCodec<RegistryByteBuf, TerritoryTrustAddPayload> CODEC =
            PacketCodec.of(
                    TerritoryTrustAddPayload::write,
                    TerritoryTrustAddPayload::read
            );

    public TerritoryTrustAddPayload(BlockPos anchorPos, String playerName) {
        this(anchorPos, playerName, false);
    }

    private void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, anchorPos);
        buf.writeString(playerName);
        buf.writeBoolean(connectedArea);
    }

    private static TerritoryTrustAddPayload read(RegistryByteBuf buf) {
        return new TerritoryTrustAddPayload(
                BlockPos.PACKET_CODEC.decode(buf),
                buf.readString(),
                buf.readBoolean()
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
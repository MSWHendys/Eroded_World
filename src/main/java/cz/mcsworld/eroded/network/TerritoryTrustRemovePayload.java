package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record TerritoryTrustRemovePayload(
        BlockPos anchorPos,
        String targetUuid,
        String targetName,
        boolean connectedArea
) implements CustomPayload {

    public static final Id<TerritoryTrustRemovePayload> ID =
            new Id<>(Identifier.of(ErodedMod.MOD_ID, "territory_trust_remove"));

    public static final PacketCodec<RegistryByteBuf, TerritoryTrustRemovePayload> CODEC =
            PacketCodec.of(
                    TerritoryTrustRemovePayload::write,
                    TerritoryTrustRemovePayload::read
            );

    public TerritoryTrustRemovePayload(BlockPos anchorPos, String targetUuid, String targetName) {
        this(anchorPos, targetUuid, targetName, false);
    }

    private void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, anchorPos);
        buf.writeString(targetUuid);
        buf.writeString(targetName);
        buf.writeBoolean(connectedArea);
    }

    private static TerritoryTrustRemovePayload read(RegistryByteBuf buf) {
        return new TerritoryTrustRemovePayload(
                BlockPos.PACKET_CODEC.decode(buf),
                buf.readString(),
                buf.readString(),
                buf.readBoolean()
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
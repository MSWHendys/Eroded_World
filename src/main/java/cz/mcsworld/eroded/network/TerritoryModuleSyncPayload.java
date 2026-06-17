package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record TerritoryModuleSyncPayload(
        BlockPos anchorPos,
        String ownerName,
        int radius,
        int active,
        int connectedWidth,
        int connectedDepth,
        int connectedClaimCount,
        String trustedData,
        String suggestionData
) implements CustomPayload {

    public static final Id<TerritoryModuleSyncPayload> ID =
            new Id<>(Identifier.of(ErodedMod.MOD_ID, "territory_module_sync"));

    public static final PacketCodec<RegistryByteBuf, TerritoryModuleSyncPayload> CODEC =
            PacketCodec.of(
                    TerritoryModuleSyncPayload::write,
                    TerritoryModuleSyncPayload::read
            );

    private void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, anchorPos);
        buf.writeString(ownerName);
        buf.writeInt(radius);
        buf.writeInt(active);

        buf.writeInt(connectedWidth);
        buf.writeInt(connectedDepth);
        buf.writeInt(connectedClaimCount);

        buf.writeString(trustedData);
        buf.writeString(suggestionData);
    }

    private static TerritoryModuleSyncPayload read(RegistryByteBuf buf) {
        BlockPos anchorPos = BlockPos.PACKET_CODEC.decode(buf);
        String ownerName = buf.readString();
        int radius = buf.readInt();
        int active = buf.readInt();

        int connectedWidth = buf.readInt();
        int connectedDepth = buf.readInt();
        int connectedClaimCount = buf.readInt();

        String trustedData = buf.readString();
        String suggestionData = buf.readString();

        return new TerritoryModuleSyncPayload(
                anchorPos,
                ownerName,
                radius,
                active,
                connectedWidth,
                connectedDepth,
                connectedClaimCount,
                trustedData,
                suggestionData
        );
    }

    public boolean isActive() {
        return active == 1;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
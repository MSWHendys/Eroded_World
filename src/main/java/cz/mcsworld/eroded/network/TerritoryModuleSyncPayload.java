package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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
) implements CustomPacketPayload {

    public static final Type<TerritoryModuleSyncPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_module_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerritoryModuleSyncPayload> CODEC =
            StreamCodec.ofMember(
                    TerritoryModuleSyncPayload::write,
                    TerritoryModuleSyncPayload::read
            );

    private void write(RegistryFriendlyByteBuf buf) {
        BlockPos.STREAM_CODEC.encode(buf, anchorPos);
        buf.writeUtf(ownerName);
        buf.writeInt(radius);
        buf.writeInt(active);

        buf.writeInt(connectedWidth);
        buf.writeInt(connectedDepth);
        buf.writeInt(connectedClaimCount);

        buf.writeUtf(trustedData);
        buf.writeUtf(suggestionData);
    }

    private static TerritoryModuleSyncPayload read(RegistryFriendlyByteBuf buf) {
        BlockPos anchorPos = BlockPos.STREAM_CODEC.decode(buf);
        String ownerName = buf.readUtf();
        int radius = buf.readInt();
        int active = buf.readInt();

        int connectedWidth = buf.readInt();
        int connectedDepth = buf.readInt();
        int connectedClaimCount = buf.readInt();

        String trustedData = buf.readUtf();
        String suggestionData = buf.readUtf();

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
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
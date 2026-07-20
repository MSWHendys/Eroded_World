package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerritoryPermissionUpdatePayload(
        BlockPos anchorPos,
        String targetUuid,
        String permissionName,
        boolean enabled,
        boolean connectedArea
) implements CustomPacketPayload {

    public static final Type<TerritoryPermissionUpdatePayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_permission_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerritoryPermissionUpdatePayload> CODEC =
            StreamCodec.ofMember(
                    TerritoryPermissionUpdatePayload::write,
                    TerritoryPermissionUpdatePayload::read
            );

    private void write(RegistryFriendlyByteBuf buf) {
        BlockPos.STREAM_CODEC.encode(buf, anchorPos);
        buf.writeUtf(targetUuid);
        buf.writeUtf(permissionName);
        buf.writeBoolean(enabled);
        buf.writeBoolean(connectedArea);
    }

    private static TerritoryPermissionUpdatePayload read(RegistryFriendlyByteBuf buf) {
        return new TerritoryPermissionUpdatePayload(
                BlockPos.STREAM_CODEC.decode(buf),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
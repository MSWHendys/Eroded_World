package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record TerritoryPermissionUpdatePayload(
        BlockPos anchorPos,
        String targetUuid,
        String permissionName,
        boolean enabled,
        boolean connectedArea
) implements CustomPayload {

    public static final Id<TerritoryPermissionUpdatePayload> ID =
            new Id<>(Identifier.of(ErodedMod.MOD_ID, "territory_permission_update"));

    public static final PacketCodec<RegistryByteBuf, TerritoryPermissionUpdatePayload> CODEC =
            PacketCodec.of(
                    TerritoryPermissionUpdatePayload::write,
                    TerritoryPermissionUpdatePayload::read
            );

    private void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, anchorPos);
        buf.writeString(targetUuid);
        buf.writeString(permissionName);
        buf.writeBoolean(enabled);
        buf.writeBoolean(connectedArea);
    }

    private static TerritoryPermissionUpdatePayload read(RegistryByteBuf buf) {
        return new TerritoryPermissionUpdatePayload(
                BlockPos.PACKET_CODEC.decode(buf),
                buf.readString(),
                buf.readString(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
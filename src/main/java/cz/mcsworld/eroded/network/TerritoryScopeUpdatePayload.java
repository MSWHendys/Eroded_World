package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TerritoryScopeUpdatePayload(
        BlockPos anchorPos,
        String targetUuid,
        boolean connectedScopeMode
) implements CustomPacketPayload {

    public static final int MAX_UUID_LENGTH = 36;

    public static final Type<TerritoryScopeUpdatePayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_scope_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerritoryScopeUpdatePayload> CODEC =
            StreamCodec.ofMember(
                    TerritoryScopeUpdatePayload::write,
                    TerritoryScopeUpdatePayload::read
            );

    private void write(RegistryFriendlyByteBuf buf) {
        BlockPos.STREAM_CODEC.encode(buf, anchorPos);
        buf.writeUtf(targetUuid, MAX_UUID_LENGTH);
        buf.writeBoolean(connectedScopeMode);
    }

    private static TerritoryScopeUpdatePayload read(RegistryFriendlyByteBuf buf) {
        return new TerritoryScopeUpdatePayload(
                BlockPos.STREAM_CODEC.decode(buf),
                buf.readUtf(MAX_UUID_LENGTH),
                buf.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
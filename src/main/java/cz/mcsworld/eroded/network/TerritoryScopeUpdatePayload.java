package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record TerritoryScopeUpdatePayload(
        BlockPos anchorPos,
        String targetUuid,
        boolean connectedScopeMode
) implements CustomPayload {

    public static final Id<TerritoryScopeUpdatePayload> ID =
            new Id<>(Identifier.of(ErodedMod.MOD_ID, "territory_scope_update"));

    public static final PacketCodec<RegistryByteBuf, TerritoryScopeUpdatePayload> CODEC =
            PacketCodec.of(
                    TerritoryScopeUpdatePayload::write,
                    TerritoryScopeUpdatePayload::read
            );

    private void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, anchorPos);
        buf.writeString(targetUuid);
        buf.writeBoolean(connectedScopeMode);
    }

    private static TerritoryScopeUpdatePayload read(RegistryByteBuf buf) {
        return new TerritoryScopeUpdatePayload(
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
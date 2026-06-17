package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record TerritorySuggestionRequestPayload(
        BlockPos anchorPos,
        String query
) implements CustomPayload {

    public static final Id<TerritorySuggestionRequestPayload> ID =
            new Id<>(Identifier.of(ErodedMod.MOD_ID, "territory_suggestion_request"));

    public static final PacketCodec<RegistryByteBuf, TerritorySuggestionRequestPayload> CODEC =
            PacketCodec.of(
                    TerritorySuggestionRequestPayload::write,
                    TerritorySuggestionRequestPayload::read
            );

    private void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, anchorPos);
        buf.writeString(query);
    }

    private static TerritorySuggestionRequestPayload read(RegistryByteBuf buf) {
        return new TerritorySuggestionRequestPayload(
                BlockPos.PACKET_CODEC.decode(buf),
                buf.readString()
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
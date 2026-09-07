package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerritorySuggestionRequestPayload(
        BlockPos anchorPos,
        String query
) implements CustomPacketPayload {

    public static final int MAX_QUERY_LENGTH = 32;

    public static final Type<TerritorySuggestionRequestPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_suggestion_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerritorySuggestionRequestPayload> CODEC =
            StreamCodec.ofMember(
                    TerritorySuggestionRequestPayload::write,
                    TerritorySuggestionRequestPayload::read
            );

    private void write(RegistryFriendlyByteBuf buf) {
        BlockPos.STREAM_CODEC.encode(buf, anchorPos);
        buf.writeUtf(query, MAX_QUERY_LENGTH);
    }

    private static TerritorySuggestionRequestPayload read(RegistryFriendlyByteBuf buf) {
        return new TerritorySuggestionRequestPayload(
                BlockPos.STREAM_CODEC.decode(buf),
                buf.readUtf(MAX_QUERY_LENGTH)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
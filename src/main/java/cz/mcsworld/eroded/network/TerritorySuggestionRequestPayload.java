package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record TerritorySuggestionRequestPayload(
        BlockPos anchorPos,
        String query
) implements CustomPacketPayload {

    public static final Type<@NotNull TerritorySuggestionRequestPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_suggestion_request"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull TerritorySuggestionRequestPayload> CODEC =
            StreamCodec.ofMember(
                    TerritorySuggestionRequestPayload::write,
                    TerritorySuggestionRequestPayload::read
            );

    private void write(RegistryFriendlyByteBuf buf) {
        BlockPos.STREAM_CODEC.encode(buf, anchorPos);
        buf.writeUtf(query);
    }

    private static TerritorySuggestionRequestPayload read(RegistryFriendlyByteBuf buf) {
        return new TerritorySuggestionRequestPayload(
                BlockPos.STREAM_CODEC.decode(buf),
                buf.readUtf()
        );
    }

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
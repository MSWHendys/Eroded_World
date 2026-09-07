package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record TerritoryTrustAddPayload(
        BlockPos anchorPos,
        String playerName,
        boolean connectedArea
) implements CustomPacketPayload {

    public static final int MAX_PLAYER_NAME_LENGTH = 16;

    public static final Type<@NotNull TerritoryTrustAddPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_trust_add"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull TerritoryTrustAddPayload> CODEC =
            StreamCodec.ofMember(
                    TerritoryTrustAddPayload::write,
                    TerritoryTrustAddPayload::read
            );

    public TerritoryTrustAddPayload(BlockPos anchorPos, String playerName) {
        this(anchorPos, playerName, false);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        BlockPos.STREAM_CODEC.encode(buf, anchorPos);
        buf.writeUtf(playerName, MAX_PLAYER_NAME_LENGTH);
        buf.writeBoolean(connectedArea);
    }

    private static TerritoryTrustAddPayload read(RegistryFriendlyByteBuf buf) {
        return new TerritoryTrustAddPayload(
                BlockPos.STREAM_CODEC.decode(buf),
                buf.readUtf(MAX_PLAYER_NAME_LENGTH),
                buf.readBoolean()
        );
    }

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
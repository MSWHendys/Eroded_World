package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerritoryTrustAddPayload(
        BlockPos anchorPos,
        String playerName,
        boolean connectedArea
) implements CustomPacketPayload {

    public static final int MAX_PLAYER_NAME_LENGTH = 16;

    public static final Type<TerritoryTrustAddPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_trust_add"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerritoryTrustAddPayload> CODEC =
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
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
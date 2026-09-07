package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record TerritoryTrustRemovePayload(
        BlockPos anchorPos,
        String targetUuid,
        String targetName,
        boolean connectedArea
) implements CustomPacketPayload {

    public static final int MAX_UUID_LENGTH = 36;
    public static final int MAX_TARGET_NAME_LENGTH = 16;

    public static final Type<@NotNull TerritoryTrustRemovePayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_trust_remove"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull TerritoryTrustRemovePayload> CODEC =
            StreamCodec.ofMember(
                    TerritoryTrustRemovePayload::write,
                    TerritoryTrustRemovePayload::read
            );

    public TerritoryTrustRemovePayload(BlockPos anchorPos, String targetUuid, String targetName) {
        this(anchorPos, targetUuid, targetName, false);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        BlockPos.STREAM_CODEC.encode(buf, anchorPos);
        buf.writeUtf(targetUuid, MAX_UUID_LENGTH);
        buf.writeUtf(targetName, MAX_TARGET_NAME_LENGTH);
        buf.writeBoolean(connectedArea);
    }

    private static TerritoryTrustRemovePayload read(RegistryFriendlyByteBuf buf) {
        return new TerritoryTrustRemovePayload(
                BlockPos.STREAM_CODEC.decode(buf),
                buf.readUtf(MAX_UUID_LENGTH),
                buf.readUtf(MAX_TARGET_NAME_LENGTH),
                buf.readBoolean()
        );
    }

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
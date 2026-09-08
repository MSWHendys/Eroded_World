package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record TerritoryPlacementHintPayload(boolean showMessage) implements CustomPacketPayload {

    public static final Type<@NotNull TerritoryPlacementHintPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_placement_hint"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull TerritoryPlacementHintPayload> CODEC =
            StreamCodec.ofMember(
                    TerritoryPlacementHintPayload::write,
                    TerritoryPlacementHintPayload::read
            );

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(showMessage);
    }

    private static TerritoryPlacementHintPayload read(RegistryFriendlyByteBuf buf) {
        return new TerritoryPlacementHintPayload(buf.readBoolean());
    }

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }
}
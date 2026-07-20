package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerritoryPlacementHintPayload(boolean showMessage) implements CustomPacketPayload {

    public static final Type<TerritoryPlacementHintPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_placement_hint"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerritoryPlacementHintPayload> CODEC =
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
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TerritoryPlacementHintPayload(boolean showMessage) implements CustomPayload {

    public static final Id<TerritoryPlacementHintPayload> ID =
            new Id<>(Identifier.of(ErodedMod.MOD_ID, "territory_placement_hint"));

    public static final PacketCodec<RegistryByteBuf, TerritoryPlacementHintPayload> CODEC =
            PacketCodec.of(
                    TerritoryPlacementHintPayload::write,
                    TerritoryPlacementHintPayload::read
            );

    private void write(RegistryByteBuf buf) {
        buf.writeBoolean(showMessage);
    }

    private static TerritoryPlacementHintPayload read(RegistryByteBuf buf) {
        return new TerritoryPlacementHintPayload(buf.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
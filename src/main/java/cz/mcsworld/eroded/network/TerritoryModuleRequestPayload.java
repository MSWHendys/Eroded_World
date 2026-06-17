package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record TerritoryModuleRequestPayload(BlockPos anchorPos) implements CustomPayload {

    public static final Id<TerritoryModuleRequestPayload> ID =
            new Id<>(Identifier.of(ErodedMod.MOD_ID, "territory_module_request"));

    public static final PacketCodec<RegistryByteBuf, TerritoryModuleRequestPayload> CODEC =
            PacketCodec.of(
                    TerritoryModuleRequestPayload::write,
                    TerritoryModuleRequestPayload::read
            );

    private void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, anchorPos);
    }

    private static TerritoryModuleRequestPayload read(RegistryByteBuf buf) {
        return new TerritoryModuleRequestPayload(BlockPos.PACKET_CODEC.decode(buf));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
package cz.mcsworld.eroded.screen;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

public record TerritoryModuleScreenData(BlockPos anchorPos) {

    public static final PacketCodec<RegistryByteBuf, TerritoryModuleScreenData> PACKET_CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC,
                    TerritoryModuleScreenData::anchorPos,
                    TerritoryModuleScreenData::new
            );
}
package cz.mcsworld.eroded.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public record TerritoryModuleScreenData(BlockPos anchorPos) {

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull TerritoryModuleScreenData> PACKET_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    TerritoryModuleScreenData::anchorPos,
                    TerritoryModuleScreenData::new
            );
}
package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TerritoryModuleRequestPayload(BlockPos anchorPos) implements CustomPacketPayload {

    public static final Type<TerritoryModuleRequestPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "territory_module_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerritoryModuleRequestPayload> CODEC =
            StreamCodec.ofMember(
                    TerritoryModuleRequestPayload::write,
                    TerritoryModuleRequestPayload::read
            );

    private void write(RegistryFriendlyByteBuf buf) {
        BlockPos.STREAM_CODEC.encode(buf, anchorPos);
    }

    private static TerritoryModuleRequestPayload read(RegistryFriendlyByteBuf buf) {
        return new TerritoryModuleRequestPayload(BlockPos.STREAM_CODEC.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class TerritoryPlacementHintNetworking {

    private TerritoryPlacementHintNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(
                TerritoryPlacementHintPayload.ID,
                TerritoryPlacementHintPayload.CODEC
        );
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryPlacementHintPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();

                    if (!(player.getWorld() instanceof ServerWorld world)) {
                        return;
                    }

                    TerritoryProtectionManager.showNextAnchorSuggestions(
                            world,
                            player,
                            payload.showMessage()
                    );
                })
        );
    }
}
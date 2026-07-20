package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class TerritoryPlacementHintNetworking {

    private TerritoryPlacementHintNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(
                TerritoryPlacementHintPayload.ID,
                TerritoryPlacementHintPayload.CODEC
        );
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryPlacementHintPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer player = context.player();

                    if (!(player.level() instanceof ServerLevel world)) {
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
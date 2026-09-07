package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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
                (payload, context) -> {
                    ServerPlayer player = context.player();

                    if (!ServerPacketGuard.allow(player, "territory_hint", 3)) {
                        return;
                    }

                    if (!(player.level() instanceof ServerLevel world)) {
                        return;
                    }

                    // Do not let a forged packet trigger claim scans/heightmap
                    // lookups unless the player is actually holding the item
                    // that owns this feature.
                    if (!isHoldingTerritoryHintItem(player)) {
                        return;
                    }

                    TerritoryProtectionManager.showNextAnchorSuggestions(
                            world,
                            player,
                            payload.showMessage()
                    );
                }
        );
    }

    private static boolean isHoldingTerritoryHintItem(ServerPlayer player) {
        return isHintItem(player.getMainHandItem()) || isHintItem(player.getOffhandItem());
    }

    private static boolean isHintItem(ItemStack stack) {
        return stack.is(ErodedItems.TERRITORY_MODULE)
                || stack.is(ErodedBlocks.TERRITORY_ANCHOR.asItem());
    }
}

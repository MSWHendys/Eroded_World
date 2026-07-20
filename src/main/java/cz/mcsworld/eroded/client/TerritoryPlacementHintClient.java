package cz.mcsworld.eroded.client;

import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.network.TerritoryPlacementHintPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class TerritoryPlacementHintClient {

    private static final long HOLD_DELAY_MS = 5000;
    private static final long REPEAT_INTERVAL_MS = 1000;

    private static ResourceLocation currentHeldItemId = null;
    private static long holdStartedAt = 0L;
    private static long lastRequestAt = 0L;

    private static boolean messageSentForCurrentHold = false;

    private TerritoryPlacementHintClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                resetHold();
                return;
            }

            ItemStack mainHand = client.player.getMainHandItem();
            ItemStack offHand = client.player.getOffhandItem();

            ItemStack territoryStack = ItemStack.EMPTY;

            if (isTerritoryHintItem(mainHand)) {
                territoryStack = mainHand;
            } else if (isTerritoryHintItem(offHand)) {
                territoryStack = offHand;
            }

            if (territoryStack.isEmpty()) {
                resetHold();
                return;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(territoryStack.getItem());
            long now = System.currentTimeMillis();

            if (!itemId.equals(currentHeldItemId)) {
                currentHeldItemId = itemId;
                holdStartedAt = now;
                lastRequestAt = 0L;
                messageSentForCurrentHold = false;
                return;
            }

            if (now - holdStartedAt < HOLD_DELAY_MS) {
                return;
            }

            if (now - lastRequestAt < REPEAT_INTERVAL_MS) {
                return;
            }

            boolean showMessage = !messageSentForCurrentHold;

            ClientPlayNetworking.send(new TerritoryPlacementHintPayload(showMessage));

            messageSentForCurrentHold = true;
            lastRequestAt = now;
        });
    }

    private static void resetHold() {
        currentHeldItemId = null;
        holdStartedAt = 0L;
        lastRequestAt = 0L;
        messageSentForCurrentHold = false;
    }

    private static boolean isTerritoryHintItem(ItemStack stack) {
        return stack.is(ErodedItems.TERRITORY_MODULE)
                || stack.is(ErodedBlocks.TERRITORY_ANCHOR.asItem());
    }
}
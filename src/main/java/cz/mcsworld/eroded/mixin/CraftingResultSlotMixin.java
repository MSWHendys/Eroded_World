package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.crafting.CraftingService;
import cz.mcsworld.eroded.crafting.context.CraftingContext;
import cz.mcsworld.eroded.crafting.context.CraftingContextFactory;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.network.CraftingRequirementPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {

    @Unique
    private static final int ERODED_REQUIRED_LEVEL = 10;

    @Unique
    private static final Map<UUID, Long> ERODED_LEVEL_MESSAGE_COOLDOWN = new ConcurrentHashMap<>();

    @Inject(
            method = "takeStack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$blockSpecialCraftBeforeTake(
            int amount,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        CraftingResultSlot self = (CraftingResultSlot) (Object) this;
        ItemStack result = self.getStack();

        if (!eroded$requiresLevel10(result)) {
            return;
        }

        PlayerEntity player = ((CraftingResultSlotAccessor) self).eroded$getPlayer();

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        if (serverPlayer.experienceLevel >= ERODED_REQUIRED_LEVEL) {
            return;
        }

        eroded$sendLevelMessage(serverPlayer);

        cir.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(
            method = "onTakeItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$onTakeItem(
            PlayerEntity player,
            ItemStack stack,
            CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;

        CraftingResultSlot self = (CraftingResultSlot) (Object) this;

        if (eroded$requiresLevel10(stack) && serverPlayer.experienceLevel < ERODED_REQUIRED_LEVEL) {
            eroded$sendLevelMessage(serverPlayer);
            stack.setCount(0);
            ci.cancel();
            return;
        }

        RecipeInputInventory inputInv =
                ((CraftingResultSlotAccessor) self).eroded$getInput();

        Recipe<?> recipe = null;

        CraftingContext context =
                CraftingContextFactory.create(serverPlayer, recipe, inputInv);

        boolean success = CraftingService.process(context, stack);

        if (!success) {
            stack.setCount(0);
            ci.cancel();
        }
    }

    @Unique
    private static boolean eroded$requiresLevel10(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        return item == ErodedBlocks.WARDING_LANTERN.asItem()
                || item == ErodedItems.ENERGY_DRINK
                || item == ErodedItems.ADRENALINE_SHOT;
    }
    @Unique
    private static void eroded$sendLevelMessage(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        long last = ERODED_LEVEL_MESSAGE_COOLDOWN.getOrDefault(player.getUuid(), 0L);

        if (now - last < 1000L) {
            return;
        }

        ERODED_LEVEL_MESSAGE_COOLDOWN.put(player.getUuid(), now);

        SafeNetworkUtil.safeSend(
                player,
                new CraftingRequirementPacket("eroded.crafting.requires_level_10")
        );
    }
}
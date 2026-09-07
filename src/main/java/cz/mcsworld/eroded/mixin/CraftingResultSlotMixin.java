package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.crafting.CraftingService;
import cz.mcsworld.eroded.crafting.CraftingMessageCooldown;
import cz.mcsworld.eroded.crafting.context.CraftingContext;
import cz.mcsworld.eroded.crafting.context.CraftingContextFactory;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.network.CraftingRequirementPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

@Mixin(ResultSlot.class)
public class CraftingResultSlotMixin {

    @Unique
    private static final int ERODED_REQUIRED_LEVEL = 10;

    @Inject(
            method = "remove",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$blockSpecialCraftBeforeTake(
            int amount,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!CraftingConfig.get().enabled) return;

        ResultSlot self = (ResultSlot) (Object) this;
        ItemStack result = self.getItem();

        if (!eroded$requiresLevel10(result)) {
            return;
        }

        Player player = ((CraftingResultSlotAccessor) self).eroded$getPlayer();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (serverPlayer.experienceLevel >= ERODED_REQUIRED_LEVEL) {
            return;
        }

        eroded$sendLevelMessage(serverPlayer);

        cir.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(
            method = "onTake",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$onTakeItem(
            Player player,
            ItemStack stack,
            CallbackInfo ci
    ) {
        if (!CraftingConfig.get().enabled) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;

        ResultSlot self = (ResultSlot) (Object) this;

        if (eroded$requiresLevel10(stack) && serverPlayer.experienceLevel < ERODED_REQUIRED_LEVEL) {
            eroded$sendLevelMessage(serverPlayer);
            stack.setCount(0);
            ci.cancel();
            return;
        }

        CraftingContainer inputInv =
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
    private static void eroded$sendLevelMessage(ServerPlayer player) {
        long now = System.currentTimeMillis();
        if (!CraftingMessageCooldown.tryAcquire(player.getUUID(), now, 1000L)) {
            return;
        }

        SafeNetworkUtil.safeSend(
                player,
                new CraftingRequirementPacket("eroded.crafting.requires_level_10")
        );
    }
}
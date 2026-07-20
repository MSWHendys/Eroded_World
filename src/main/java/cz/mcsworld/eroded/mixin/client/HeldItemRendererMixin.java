package cz.mcsworld.eroded.mixin.client;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private ItemStack offHandItem;

    @Inject(method = "tick", at = @At("HEAD"))
    private void eroded$preventTimedLightBobbing(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null) {
            return;
        }

        ItemStack currentMainStack = player.getMainHandItem();

        if (isStableTimedLightItem(currentMainStack)
                && isStableTimedLightItem(this.mainHandItem)
                && ItemStack.isSameItem(currentMainStack, this.mainHandItem)) {

            this.mainHandItem = currentMainStack;
        }

        ItemStack currentOffStack = player.getOffhandItem();

        if (isStableTimedLightItem(currentOffStack)
                && isStableTimedLightItem(this.offHandItem)
                && ItemStack.isSameItem(currentOffStack, this.offHandItem)) {

            this.offHandItem = currentOffStack;
        }
    }

    @Unique
    private boolean isStableTimedLightItem(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (
                stack.is(ErodedBlocks.WARDING_LANTERN.asItem())
                        || stack.is(ErodedBlocks.ERODED_TORCH_ITEM)
        );
    }
}
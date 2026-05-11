package cz.mcsworld.eroded.mixin.client;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Shadow private ItemStack mainHand;
    @Shadow private ItemStack offHand;

    @Inject(method = "updateHeldItems", at = @At("HEAD"))
    private void eroded$preventLampBobbing(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        ItemStack currentMainStack = player.getMainHandStack();
        if (isWardingLantern(currentMainStack) && isWardingLantern(this.mainHand)) {

            if (ItemStack.areItemsEqual(currentMainStack, this.mainHand)) {
                this.mainHand = currentMainStack;
            }
        }

        ItemStack currentOffStack = player.getOffHandStack();
        if (isWardingLantern(currentOffStack) && isWardingLantern(this.offHand)) {
            if (ItemStack.areItemsEqual(currentOffStack, this.offHand)) {
                this.offHand = currentOffStack;
            }
        }
    }

    private boolean isWardingLantern(ItemStack stack) {
        return stack != null && stack.isOf(ErodedBlocks.WARDING_LANTERN.asItem());
    }
}
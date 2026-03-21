package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import net.minecraft.util.hit.BlockHitResult;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class PlayerBlockPlaceMixin {

    @Shadow
    @Final
    protected ServerPlayerEntity player;

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void eroded$preventPlacement(
            ServerPlayerEntity player,
            World world,
            ItemStack stack,
            Hand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (!(stack.getItem() instanceof BlockItem)) {
            return;
        }

        BlockPos placePos = hit.getBlockPos().offset(hit.getSide());

        if (!ExplosionProtectionManager.canPlace(this.player, placePos)) {

            cir.setReturnValue(ActionResult.FAIL);

            player.getInventory().markDirty();

            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                    -2, 0, player.getInventory().getSelectedSlot(), player.getInventory().getStack(player.getInventory().getSelectedSlot())
            ));

            player.currentScreenHandler.syncState();
        }
    }
}
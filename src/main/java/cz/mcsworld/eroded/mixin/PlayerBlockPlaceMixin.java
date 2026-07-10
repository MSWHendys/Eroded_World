package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerBlockPlaceMixin {

    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void eroded$preventPlacement(
            ServerPlayer player,
            Level world,
            ItemStack stack,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!(stack.getItem() instanceof BlockItem)) {
            return;
        }

        BlockPos placePos = hit.getBlockPos().relative(hit.getDirection());

        if (!ExplosionProtectionManager.canPlace(this.player, placePos)) {

            cir.setReturnValue(InteractionResult.FAIL);

            player.getInventory().setChanged();

            player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                    -2, 0, player.getInventory().getSelectedSlot(), player.getInventory().getItem(player.getInventory().getSelectedSlot())
            ));

            player.containerMenu.sendAllDataToRemote();
        }
    }
}
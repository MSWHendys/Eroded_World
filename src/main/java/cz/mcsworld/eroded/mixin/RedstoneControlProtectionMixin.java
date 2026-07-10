package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.RedstoneProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({
        LeverBlock.class,
        ButtonBlock.class,
        DoorBlock.class,
        TrapDoorBlock.class,
        FenceGateBlock.class
})
public abstract class RedstoneControlProtectionMixin {

    @Inject(
            method = "useWithoutItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$protectRedstoneControlUse(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!RedstoneProtectionManager.canPlayerUseControl(
                serverPlayer,
                serverWorld,
                pos
        )) {

            serverPlayer.connection.send(
                    new ClientboundBlockUpdatePacket(pos, state)
            );

            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
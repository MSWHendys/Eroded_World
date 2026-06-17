package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.RedstoneProtectionManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({
        LeverBlock.class,
        ButtonBlock.class,
        DoorBlock.class,
        TrapdoorBlock.class,
        FenceGateBlock.class
})
public abstract class RedstoneControlProtectionMixin {

    @Inject(
            method = "onUse",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$protectRedstoneControlUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        if (!RedstoneProtectionManager.canPlayerUseControl(
                serverPlayer,
                serverWorld,
                pos
        )) {

            serverPlayer.networkHandler.sendPacket(
                    new BlockUpdateS2CPacket(pos, state)
            );

            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketProtectionMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void eroded$preventBucketPlacement(
            World world,
            PlayerEntity user,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {

        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(user instanceof ServerPlayerEntity player)) return;

        if (ExplosionProtectionManager.canPlace(player, player.getBlockPos())) {
            return;
        }

        BlockPos pos = player.getBlockPos();

        if (ExplosionProtectionManager.isProtected(serverWorld, pos)) {

            cir.setReturnValue(ActionResult.FAIL);

        }
    }
}
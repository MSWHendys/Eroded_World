package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketProtectionMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void eroded$preventBucketPlacement(
            Level world,
            Player user,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {

        if (!(world instanceof ServerLevel serverWorld)) return;
        if (!(user instanceof ServerPlayer player)) return;

        if (ExplosionProtectionManager.canPlace(player, player.blockPosition())) {
            return;
        }

        BlockPos pos = player.blockPosition();

        if (ExplosionProtectionManager.isProtected(serverWorld, pos)) {

            cir.setReturnValue(InteractionResult.FAIL);

        }
    }
}
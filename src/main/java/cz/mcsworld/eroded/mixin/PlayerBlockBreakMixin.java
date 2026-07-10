package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerBlockBreakMixin {

    @Shadow @Final
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void eroded$preventBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {

        if (!ExplosionProtectionManager.canBreak(player, pos)) {
            cir.setReturnValue(false);
        }
    }
}
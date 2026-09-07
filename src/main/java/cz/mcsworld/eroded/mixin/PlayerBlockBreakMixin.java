package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerBlockBreakMixin {

    @Shadow @Final
    protected ServerPlayer player;

    @Shadow
    private boolean isDestroyingBlock;

    /**
     * Passive regeneration must not run while the player is actively mining.
     * ServerPlayerGameMode owns the authoritative block-breaking state, so
     * this works for slow hand-mining as well as fast tools and does not
     * depend on client FPS or repeated client callbacks.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void eroded$pauseEnergyRegenWhileMining(CallbackInfo ci) {
        if (!isDestroyingBlock) return;
        if (player.isCreative() || player.isSpectator()) return;

        var energy = EnergyConfig.get().server;
        if (!energy.enabled
                || !energy.mining.enabled
                || !energy.mining.pausePassiveRegenWhileMining) {
            return;
        }

        SkillManager.get(player).pauseRegenerationClock();
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void eroded$preventBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {

        if (!ExplosionProtectionManager.canBreak(player, pos)) {
            cir.setReturnValue(false);
        }
    }
}

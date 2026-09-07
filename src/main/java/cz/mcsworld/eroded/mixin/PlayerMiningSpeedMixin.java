package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.energy.MiningSpeedRules;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMiningSpeedMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void eroded$scaleMiningSpeedByEnergy(
            BlockState state,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        var server = EnergyConfig.get().server;
        if (!server.enabled || !server.mining.enabled || !server.mining.speedScalingEnabled) return;

        SkillData data = SkillManager.get(player);
        float multiplier = MiningSpeedRules.multiplier(
                true,
                true,
                server.mining.allowMiningAtZero,
                data.getEnergy(),
                data.getMaxEnergy(),
                server.mining.fullSpeedFromPercent,
                server.mining.reducedSpeedFromPercent,
                server.mining.reducedSpeedPercent,
                server.mining.criticalSpeedPercent
        );

        if (multiplier == 1.0f) return;
        cir.setReturnValue(cir.getReturnValue() * multiplier);
    }
}

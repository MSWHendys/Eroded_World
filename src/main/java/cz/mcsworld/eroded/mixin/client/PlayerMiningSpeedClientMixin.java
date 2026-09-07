package cz.mcsworld.eroded.mixin.client;

import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.energy.MiningSpeedRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Player.class)
public abstract class PlayerMiningSpeedClientMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void eroded$scaleLocalMiningSpeedByEnergy(
            BlockState state,
            CallbackInfoReturnable<Float> cir
    ) {
        Player self = (Player) (Object) this;
        if (Minecraft.getInstance().player != self) return;
        if (!ClientEnergyData.isInitialized()) return;
        if (self.isCreative() || self.isSpectator()) return;

        float multiplier = MiningSpeedRules.multiplier(
                ClientEnergyData.isEnabled() && ClientEnergyData.isMiningEnabled(),
                ClientEnergyData.isMiningSpeedScalingEnabled(),
                ClientEnergyData.isMiningAllowAtZero(),
                ClientEnergyData.getEnergy(),
                ClientEnergyData.getMaxEnergy(),
                ClientEnergyData.getMiningFullSpeedFromPercent(),
                ClientEnergyData.getMiningReducedSpeedFromPercent(),
                ClientEnergyData.getMiningReducedSpeedPercent(),
                ClientEnergyData.getMiningCriticalSpeedPercent()
        );

        if (multiplier == 1.0f) return;
        cir.setReturnValue(cir.getReturnValue() * multiplier);
    }
}

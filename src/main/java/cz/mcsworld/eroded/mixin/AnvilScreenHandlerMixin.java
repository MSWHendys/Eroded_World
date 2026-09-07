package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.crafting.*;
import cz.mcsworld.eroded.energy.EnergyCostResolver;
import cz.mcsworld.eroded.network.AnvilFeedbackPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public class AnvilScreenHandlerMixin {

    @ModifyVariable(
            method = "createResult",
            at = @At(value = "STORE"),
            ordinal = 0
    )
    private int eroded$modifyRepairAmount(int repairedAmount) {

        var root = CraftingConfig.get();
        if (!root.enabled || !root.quality.enabled) return repairedAmount;

        AnvilMenu self = (AnvilMenu) (Object) this;

        ItemStack input = self.getSlot(0).getItem();
        if (input.isEmpty()) return repairedAmount;

        Quality quality = ItemQuality.get(input);
        float multiplier = QualityRepairModifier.getRepairMultiplier(quality);

        return Math.round(repairedAmount * multiplier);
    }

    @Inject(
            method = "createResult",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$blockTooDamaged(CallbackInfo ci) {

        var root = CraftingConfig.get();
        if (!root.enabled || !root.quality.enabled) return;

        AnvilMenu self = (AnvilMenu)(Object)this;

        ItemStack input = self.getSlot(0).getItem();
        if (input.isEmpty()) return;

        if (ItemQuality.get(input) == Quality.POOR) {

            self.getSlot(2).setByPlayer(ItemStack.EMPTY);
            ci.cancel();
        }
    }

    @Inject(
            method = "onTake",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$anvilProcess(
            Player player,
            ItemStack stack,
            CallbackInfo ci
    ) {

        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;

        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack input = self.getSlot(0).getItem();
        if (input.isEmpty()) return;

        var root = CraftingConfig.get();
        if (!root.enabled) return;

        SkillData data = SkillManager.get(serverPlayer);
        var craftingCfg = root.energy;
        boolean qualityEnabled = root.quality.enabled;

        var energyRoot = EnergyConfig.get();
        var energyCfg = energyRoot.server.core;
        boolean energyEnabled = energyRoot.server.enabled && craftingCfg.enabled;

        int baseCost = EnergyCostResolver.getBaseCraftingCost(input);

        Quality workQuality = qualityEnabled
                ? QualityResolver.resolveQuality(data, 1.0f)
                : Quality.STANDARD;

        float modifier = switch (workQuality) {
            case POOR -> craftingCfg.poorQualityEnergyMultiplier;
            case STANDARD -> craftingCfg.standardQualityEnergyMultiplier;
            case EXCELLENT -> craftingCfg.excellentQualityEnergyMultiplier;
        };

        int segmentCost = Math.max(
                craftingCfg.minimumCraftCost,
                Math.round(baseCost * modifier * 1.5f)
        );

        int energyCost =
                segmentCost * Math.max(1, energyCfg.energyPerSegment);

        if (energyEnabled && !data.hasEnoughEnergy(energyCost)) {
            if (energyCfg.blockWorkAtZero) {

                SafeNetworkUtil.safeSend(
                        serverPlayer,

                        new AnvilFeedbackPacket("eroded.energy.state.empty", "NONE")
                );

                stack.setCount(0);
                ci.cancel();

                return;
            }
        }

        if (energyEnabled) {
            data.consumeEnergy(energyCost);
        }

        if (qualityEnabled) {
            Quality currentQuality = ItemQuality.get(input);

            Quality newQuality = switch (currentQuality) {
                case EXCELLENT -> Quality.STANDARD;
                case STANDARD -> Quality.POOR;
                default -> currentQuality;
            };

            CraftingQualityApplier.apply(stack, newQuality);

            SafeNetworkUtil.safeSend(
                    serverPlayer,
                    new AnvilFeedbackPacket(
                            "eroded.anvil.quality_degraded",
                            newQuality.name()
                    )
            );
        }
    }
}

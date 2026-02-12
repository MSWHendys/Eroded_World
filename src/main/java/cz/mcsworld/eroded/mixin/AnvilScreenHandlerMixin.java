package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.crafting.ItemQuality;
import cz.mcsworld.eroded.crafting.Quality;
import cz.mcsworld.eroded.crafting.QualityRepairModifier;
import cz.mcsworld.eroded.crafting.QualityResolver;
import cz.mcsworld.eroded.energy.EnergyCostResolver;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.util.ActionBarUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public class AnvilScreenHandlerMixin {

    @ModifyVariable(
            method = "updateResult",
            at = @At(value = "STORE"),
            ordinal = 0
    )
    private int eroded$modifyRepairAmount(int repairedAmount) {

        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;

        ItemStack input = self.getSlot(0).getStack();
        if (input.isEmpty()) return repairedAmount;

        Quality quality = ItemQuality.get(input);
        float multiplier = QualityRepairModifier.getRepairMultiplier(quality);

        return Math.round(repairedAmount * multiplier);
    }

    @Inject(
            method = "onTakeOutput",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$anvilEnergyCost(
            PlayerEntity player,
            ItemStack stack,
            CallbackInfo ci
    ) {

        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;

        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;
        ItemStack input = self.getSlot(0).getStack();
        if (input.isEmpty()) return;

        Quality currentQuality = ItemQuality.get(input);

        if (currentQuality == Quality.POOR) {
            ActionBarUtil.send(
                    serverPlayer,
                    Text.translatable("eroded.anvil.too_damaged")
            );
            ci.cancel();
            return;
        }

        SkillData data = SkillManager.get(serverPlayer);

        EnergyConfig energyCfg = EnergyConfig.get();
        CraftingConfig craftingCfg = CraftingConfig.get();

        int baseCost = EnergyCostResolver.getBaseCraftingCost(input);

        Quality workQuality = QualityResolver.resolveQuality(data, 1.0f);

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

        if (!data.hasEnoughEnergy(energyCost)) {
            if (energyCfg.blockWorkAtZero) {
                stack.setCount(0);
                ci.cancel();
                return;
            }
        }

        data.consumeEnergy(energyCost);

        Quality newQuality = switch (currentQuality) {
            case EXCELLENT -> Quality.STANDARD;
            case STANDARD -> Quality.POOR;
            default -> currentQuality;
        };

        ItemQuality.set(stack, newQuality);

        ActionBarUtil.send(
                serverPlayer,
                Text.translatable(
                        "eroded.anvil.quality_degraded",
                        newQuality.name()
                )
        );
    }
}

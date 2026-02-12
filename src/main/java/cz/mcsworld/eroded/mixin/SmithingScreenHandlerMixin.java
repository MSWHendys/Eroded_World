package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.crafting.Quality;
import cz.mcsworld.eroded.crafting.QualityResolver;
import cz.mcsworld.eroded.energy.EnergyCostResolver;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingScreenHandler.class)
public class SmithingScreenHandlerMixin {

    @Inject(
            method = "onTakeOutput",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$smithingEnergyCost(
            PlayerEntity player,
            ItemStack stack,
            CallbackInfo ci
    ) {

        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;

        SkillData data = SkillManager.get(serverPlayer);

        EnergyConfig energyCfg = EnergyConfig.get();
        CraftingConfig craftingCfg = CraftingConfig.get();

        int baseCost = EnergyCostResolver.getBaseCraftingCost(stack);

        Quality quality = QualityResolver.resolveQuality(data, 1.0f);

        float modifier = switch (quality) {
            case POOR -> craftingCfg.poorQualityEnergyMultiplier;
            case STANDARD -> craftingCfg.standardQualityEnergyMultiplier;
            case EXCELLENT -> craftingCfg.excellentQualityEnergyMultiplier;
        };

        int segmentCost = Math.max(
                craftingCfg.minimumCraftCost,
                Math.round(baseCost * modifier * 2.0f)
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
    }
}

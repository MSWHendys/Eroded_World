package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.crafting.Quality;
import cz.mcsworld.eroded.crafting.QualityResolver;
import cz.mcsworld.eroded.energy.EnergyCostResolver;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public class SmithingScreenHandlerMixin {

    @Inject(
            method = "onTake",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$smithingEnergyCost(
            Player player,
            ItemStack stack,
            CallbackInfo ci
    ) {

        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;

        SkillData data = SkillManager.get(serverPlayer);

        var energyRoot = EnergyConfig.get();
        var energyCfg = energyRoot.server.core;

        var root = CraftingConfig.get();
        var craftingCfg = root.energy;
        if (!root.enabled || !energyRoot.server.enabled || !craftingCfg.enabled) return;

        int baseCost = EnergyCostResolver.getBaseCraftingCost(stack);

        Quality quality = root.quality.enabled
                ? QualityResolver.resolveQuality(data, 1.0f)
                : Quality.STANDARD;

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

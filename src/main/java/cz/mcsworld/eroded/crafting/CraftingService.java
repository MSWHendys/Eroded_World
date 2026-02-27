package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.crafting.context.CraftingContext;
import cz.mcsworld.eroded.energy.EnergyCostResolver;
import cz.mcsworld.eroded.network.CraftingFailPacket;
import cz.mcsworld.eroded.network.EnergySyncPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.skills.SkillType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CraftingService {

    private CraftingService() {}

    public static boolean process(CraftingContext context, ItemStack result) {

        CraftingConfig cfg = CraftingConfig.get();
        if (!cfg.enabled) return true;

        ServerPlayerEntity player = context.getPlayer();
        SkillData data = SkillManager.get(player);

        var root = EnergyConfig.get();
        var energyCfg = root.server.core;

        float avgInputQuality = InputQualityResolver.resolveAverage(context.getInputs());
        RecipeDifficulty difficulty = RecipeDifficultyResolver.resolve(result);
        boolean applyQuality = QualityApplicable.isApplicable(result);

        float diffEnergyMult = switch (difficulty) {
            case SIMPLE  -> cfg.energy.simpleRecipeEnergyMultiplier;
            case NORMAL  -> cfg.energy.normalRecipeEnergyMultiplier;
            case COMPLEX -> cfg.energy.complexRecipeEnergyMultiplier;
        };

        int rawBase = EnergyCostResolver.getBaseCraftingCostFromInputs(context.getInputs());
        int baseCost = Math.round(rawBase * diffEnergyMult);

        Quality quality = Quality.STANDARD;
        if (cfg.quality.enabled && applyQuality) {
            quality = QualityResolver.resolveQuality(data, avgInputQuality);
        }

        float qualityEnergyMult = switch (quality) {
            case POOR      -> cfg.energy.poorQualityEnergyMultiplier;
            case STANDARD  -> cfg.energy.standardQualityEnergyMultiplier;
            case EXCELLENT -> cfg.energy.excellentQualityEnergyMultiplier;
        };

        int energyCost = Math.max(
                cfg.energy.minimumCraftCost,
                Math.round(baseCost * qualityEnergyMult)
        );

        if (!cfg.energy.enabled) energyCost = 0;

        if (!data.canAffordEnergy(energyCost) && energyCfg.blockWorkAtZero) {
            SafeNetworkUtil.safeSend(player, new CraftingFailPacket());
            return false;
        }

        data.consumeEnergy(energyCost);
        SafeNetworkUtil.safeSend(player, new EnergySyncPacket(data.getEnergy()));

        if (cfg.cg.enabled) {
            SkillType skill = CraftingSkillResolver.resolve(result);
            float baseCg = CraftingCgResolver.getBaseCg(result);

            float diffCgMult = switch (difficulty) {
                case SIMPLE  -> cfg.cg.simpleRecipeCgMultiplier;
                case NORMAL  -> cfg.cg.normalRecipeCgMultiplier;
                case COMPLEX -> cfg.cg.complexRecipeCgMultiplier;
            };

            data.addCg(skill, baseCg * diffCgMult);
        }

        if (cfg.quality.enabled && applyQuality) {
            CraftingQualityApplier.apply(result, quality);
        }

        return true;
    }
}
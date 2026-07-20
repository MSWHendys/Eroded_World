package cz.mcsworld.eroded.crafting;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.crafting.context.CraftingContext;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.energy.EnergyCostResolver;
import cz.mcsworld.eroded.network.CraftingFailPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.network.SkillSyncPacket;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.skills.SkillType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CraftingService {

    private CraftingService() {}

    public static boolean process(CraftingContext context, ItemStack result) {
        CraftingConfig cfg = CraftingConfig.get();
        if (!cfg.enabled) return true;

        ServerPlayer player = context.getPlayer();
        SkillData data = SkillManager.get(player);

        if (!checkVanillaLevelRequirement(player, result)) {
            SafeNetworkUtil.safeSend(player, new CraftingFailPacket());
            return false;
        }

        var energyCfg = EnergyConfig.get().server.core;
        int energyCost = calculateEnergyCost(cfg, data, context, result);

        if (cfg.energy.enabled && energyCfg.blockWorkAtZero) {
            if (!data.canAffordEnergy(energyCost)) {
                player.displayClientMessage(
                        Component.translatable("eroded.crafting.not_enough_energy")
                                .withStyle(ChatFormatting.RED),
                        true
                );

                SafeNetworkUtil.safeSend(player, new CraftingFailPacket());
                return false;
            }
        }

        if (cfg.energy.enabled && energyCost > 0) {
            data.consumeEnergy(energyCost);
        }

        if (cfg.cg.enabled) {
            applyExperience(cfg, data, result);
        }

        SkillManager.save(player);
        SkillManager.sync(player);

        syncSkillsToClient(player, data);

        if (cfg.quality.enabled && QualityApplicable.isApplicable(result)) {
            float avgInput = InputQualityResolver.resolveAverage(context.getInputs());
            Quality quality = QualityResolver.resolveQuality(data, avgInput);
            CraftingQualityApplier.apply(result, quality);
        }

        return true;
    }

    private static boolean checkVanillaLevelRequirement(ServerPlayer player, ItemStack result) {
        Item item = result.getItem();

        boolean requiresLevel10 =
                item == ErodedBlocks.WARDING_LANTERN.asItem()
                        || item == ErodedItems.ENERGY_DRINK
                        || item == ErodedItems.ADRENALINE_SHOT;

        if (requiresLevel10 && player.experienceLevel < 10) {
            player.displayClientMessage(
                    Component.translatable("eroded.crafting.requires_level_10")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        return true;
    }

    private static int calculateEnergyCost(CraftingConfig cfg, SkillData data, CraftingContext context, ItemStack result) {
        if (!cfg.energy.enabled) return 0;

        RecipeDifficulty difficulty = RecipeDifficultyResolver.resolve(result);
        float diffMult = switch (difficulty) {
            case SIMPLE  -> cfg.energy.simpleRecipeEnergyMultiplier;
            case NORMAL  -> cfg.energy.normalRecipeEnergyMultiplier;
            case COMPLEX -> cfg.energy.complexRecipeEnergyMultiplier;
        };

        int rawBase = EnergyCostResolver.getBaseCraftingCostFromInputs(context.getInputs());
        float cost = rawBase * diffMult;

        if (cfg.quality.enabled && QualityApplicable.isApplicable(result)) {
            float avgInput = InputQualityResolver.resolveAverage(context.getInputs());
            Quality predicted = QualityResolver.resolveQuality(data, avgInput);
            cost *= switch (predicted) {
                case POOR      -> cfg.energy.poorQualityEnergyMultiplier;
                case STANDARD  -> cfg.energy.standardQualityEnergyMultiplier;
                case EXCELLENT -> cfg.energy.excellentQualityEnergyMultiplier;
            };
        }
        return Math.max(cfg.energy.minimumCraftCost, Math.round(cost));
    }

    private static void applyExperience(CraftingConfig cfg, SkillData data, ItemStack result) {
        SkillType skill = CraftingSkillResolver.resolve(result);
        RecipeDifficulty difficulty = RecipeDifficultyResolver.resolve(result);
        float baseCg = CraftingCgResolver.getBaseCg(result);

        float diffCgMult = switch (difficulty) {
            case SIMPLE  -> cfg.cg.simpleRecipeCgMultiplier;
            case NORMAL  -> cfg.cg.normalRecipeCgMultiplier;
            case COMPLEX -> cfg.cg.complexRecipeCgMultiplier;
        };
        data.addCg(skill, baseCg * diffCgMult);
    }

    private static void syncSkillsToClient(ServerPlayer player, SkillData data) {
        SafeNetworkUtil.safeSend(player, new SkillSyncPacket(
                data.getCg(SkillType.WOODWORKING),
                data.getCg(SkillType.SMELTING)
        ));
    }
}
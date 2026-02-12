package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.crafting.*;
import cz.mcsworld.eroded.energy.EnergyCostResolver;
import cz.mcsworld.eroded.network.CraftingFailPacket;
import cz.mcsworld.eroded.network.EnergySyncPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.skills.SkillType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.inventory.RecipeInputInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {

    @Inject(
            method = "onTakeItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {

        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;

        CraftingResultSlot self = (CraftingResultSlot) (Object) this;

        RecipeInputInventory inputInv = ((CraftingResultSlotAccessor) self).eroded$getInput();
        List<ItemStack> actualInputs = new ArrayList<>();
        for (int i = 0; i < inputInv.size(); i++) {
            ItemStack inputStack = inputInv.getStack(i);
            if (!inputStack.isEmpty()) {
                actualInputs.add(inputStack);
            }
        }

        SkillData data = SkillManager.get(serverPlayer);
        EnergyConfig energyCfg = EnergyConfig.get();
        CraftingConfig craftingCfg = CraftingConfig.get();

        float avgInputQuality = InputQualityResolver.resolveAverage(actualInputs);

        RecipeDifficulty difficulty = RecipeDifficultyResolver.resolve(stack);
        boolean applyQuality = QualityApplicable.isApplicable(stack);

        Quality quality = Quality.STANDARD;
        if (applyQuality) {
            quality = QualityResolver.resolveQuality(data, avgInputQuality);
        }

        float diffEnergyMult = switch (difficulty) {
            case SIMPLE  -> craftingCfg.simpleRecipeEnergyMultiplier;
            case NORMAL  -> craftingCfg.normalRecipeEnergyMultiplier;
            case COMPLEX -> craftingCfg.complexRecipeEnergyMultiplier;
        };

        int rawBase = EnergyCostResolver.getBaseCraftingCostFromInputs(actualInputs);

        int baseCost = Math.round(rawBase * diffEnergyMult);

        float qualityEnergyMult = switch (quality) {
            case POOR -> craftingCfg.poorQualityEnergyMultiplier;
            case STANDARD -> craftingCfg.standardQualityEnergyMultiplier;
            case EXCELLENT -> craftingCfg.excellentQualityEnergyMultiplier;
        };

        int energyCost = Math.max(
                craftingCfg.minimumCraftCost,
                Math.round(baseCost * qualityEnergyMult)
        );

        if (!data.canAffordEnergy(energyCost)) {
            if (energyCfg.blockWorkAtZero) {
                serverPlayer.getServer().execute(() -> {
                    SafeNetworkUtil.safeSend(serverPlayer, new CraftingFailPacket());
                });
                stack.setCount(0);
                ci.cancel();
                return;
            }
        }

        data.consumeEnergy(energyCost);
        SafeNetworkUtil.safeSend(serverPlayer, new EnergySyncPacket(data.getEnergy()));

        SkillType skill = CraftingSkillResolver.resolve(stack);
        float baseCg = CraftingCgResolver.getBaseCg(stack);
        float diffCgMult = switch (difficulty) {
            case SIMPLE  -> craftingCfg.simpleRecipeCgMultiplier;
            case NORMAL  -> craftingCfg.normalRecipeCgMultiplier;
            case COMPLEX -> craftingCfg.complexRecipeCgMultiplier;
        };

        data.addCg(skill, baseCg * diffCgMult);

        if (applyQuality) {
            CraftingQualityApplier.apply(stack, quality);
        }
    }
}
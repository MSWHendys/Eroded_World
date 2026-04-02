package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.network.EnergySyncPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.skills.SkillType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class EnergyFoodHandler {

    private EnergyFoodHandler() {}

    // ===== C TAG HELPER =====
    private static TagKey<Item> c(String path) {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of("c", path));
    }

    // ===== OPTIONAL OVERRIDE TAG =====
    private static final TagKey<Item> ERODED_OVERRIDE = tag("food_override");

    private static TagKey<Item> tag(String name) {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of("eroded", name));
    }

    // ===== MAIN =====
    public static void onEat(PlayerEntity player, ItemStack stack) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        SkillData data = SkillManager.get(serverPlayer);
        int value = calculate(stack, data);

        if (value == 0) return;

        if (value > 0) {
            data.addEnergy(value);
        } else {
            data.consumeEnergy(-value);

            // 💀 Eroded feedback
            serverPlayer.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0)
            );
        }

        SkillManager.save(serverPlayer);
        SafeNetworkUtil.safeSend(serverPlayer, new EnergySyncPacket(data.getEnergy()));
    }

    // ===== CORE =====
    private static int calculate(ItemStack stack, SkillData data) {

        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null) return 0;

        var cfg = EnergyConfig.get().server.food;

        // ===== ☠️ DANGEROUS =====
        if (stack.isIn(c("foods/food_poisoning"))) {
            return -cfg.dangerousEnergyPenalty;
        }

        float base = 0;

        // ===== 🍲 TYPE =====
        if (stack.isIn(c("foods/feasts")) || stack.isIn(c("foods/meals"))) {
            base = cfg.mealBase;

        } else if (stack.isIn(c("foods/meat"))) {
            base = cfg.meatBase;

        } else if (stack.isIn(c("foods/fish"))) {
            base = cfg.fishBase;

        } else if (stack.isIn(c("foods/grain"))) {
            base = cfg.grainBase;

        } else if (stack.isIn(c("foods/vegetables"))) {
            base = cfg.vegetableBase;

        } else if (stack.isIn(c("foods/fruits"))) {
            base = cfg.fruitBase;
        }

        // ===== fallback =====
        if (base == 0) {
            base = food.nutrition() * 0.7f;
        }

        // ===== 🔥 PROCESSING =====
        if (stack.isIn(c("foods/raw_meat"))) {
            base *= cfg.rawMultiplier;

        } else if (stack.isIn(c("foods/cooked_foods"))) {
            base *= cfg.cookedMultiplier;

        } else if (stack.isIn(c("foods/meals"))) {
            base *= cfg.processedMultiplier;
        }

        // ===== ✨ SPECIAL =====
        if (stack.isIn(c("foods/feasts"))) {
            base *= cfg.specialMultiplier;
        }

        // ===== 🧠 SKILL (SAFE) =====
        float avg =
                (data.getCg(SkillType.WOODWORKING)
                        + data.getCg(SkillType.SMELTING)) / 2f;

        base *= 1f + (avg / 400f); // max ~25%

        return Math.max(0, Math.round(base));
    }
}
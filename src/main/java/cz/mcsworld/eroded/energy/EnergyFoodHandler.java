package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.skills.SkillType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class EnergyFoodHandler {

    private EnergyFoodHandler() {}

    private static TagKey<Item> c(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }

    private static final TagKey<Item> ERODED_OVERRIDE = tag("food_override");

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("eroded", name));
    }

    public static void onEat(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!EnergyConfig.get().server.enabled) return;

        SkillData data = SkillManager.get(serverPlayer);
        int value = calculate(stack, data);

        if (value == 0) return;

        if (value > 0) {
            data.addEnergy(value);
        } else {
            data.consumeEnergy(-value);

            serverPlayer.addEffect(
                    new MobEffectInstance(MobEffects.NAUSEA, 200, 0)
            );
        }

        SkillManager.save(serverPlayer);
    }

    private static int calculate(ItemStack stack, SkillData data) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return 0;

        var cfg = EnergyConfig.get().server.food;

        if (stack.is(c("foods/food_poisoning"))) {
            return -cfg.dangerousEnergyPenalty;
        }

        float base = 0;

        if (stack.is(c("foods/feasts")) || stack.is(c("foods/meals"))) {
            base = cfg.mealBase;
        } else if (stack.is(c("foods/meat"))) {
            base = cfg.meatBase;
        } else if (stack.is(c("foods/fish"))) {
            base = cfg.fishBase;
        } else if (stack.is(c("foods/grain"))) {
            base = cfg.grainBase;
        } else if (stack.is(c("foods/vegetables"))) {
            base = cfg.vegetableBase;
        } else if (stack.is(c("foods/fruits"))) {
            base = cfg.fruitBase;
        }

        if (base == 0) {
            base = food.nutrition() * 0.7f;
        }

        if (stack.is(c("foods/raw_meat"))) {
            base *= cfg.rawMultiplier;
        } else if (stack.is(c("foods/cooked_foods"))) {
            base *= cfg.cookedMultiplier;
        } else if (stack.is(c("foods/meals"))) {
            base *= cfg.processedMultiplier;
        }

        if (stack.is(c("foods/feasts"))) {
            base *= cfg.specialMultiplier;
        }

        float avg = (data.getCg(SkillType.WOODWORKING) + data.getCg(SkillType.SMELTING)) / 2f;
        base *= 1f + (avg / 400f);

        return Math.max(0, Math.round(base));
    }
}

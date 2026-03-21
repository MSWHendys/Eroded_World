package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.network.EnergySyncPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class EnergyFoodHandler {

    private static final TagKey<Item> FD_COMFORT = TagKey.of(RegistryKeys.ITEM, Identifier.of("farmersdelight", "comfort_foods"));

    private EnergyFoodHandler() {}

    public static void onEat(PlayerEntity player, ItemStack food) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        SkillData data = SkillManager.get(serverPlayer);
        int restored = calculateEnergyFromFood(food);

        if (restored <= 0) return;

        data.addEnergy(restored);

        SafeNetworkUtil.safeSend(serverPlayer, new EnergySyncPacket(data.getEnergy()));
    }

    private static int calculateEnergyFromFood(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null) return 0;

        var cfg = EnergyConfig.get().server.food;
        int hunger = food.nutrition();
        float saturation = food.saturation();

        if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            return cfg.feastRestore;
        }

        if (stack.isIn(FD_COMFORT)) {
            return cfg.bowlRestore;
        }

        if (hunger >= 10) {
            return cfg.feastRestore;
        }

        if (hunger >= 8 && saturation >= 0.8f) {
            return cfg.bowlRestore;
        }

        if (hunger >= 5) {
            return cfg.simpleRestore;
        }

        return cfg.snackRestore;
    }
}
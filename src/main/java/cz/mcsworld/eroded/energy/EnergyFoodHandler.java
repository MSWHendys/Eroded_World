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
import net.minecraft.server.network.ServerPlayerEntity;

public final class EnergyFoodHandler {

    private EnergyFoodHandler() {}

    public static void onEat(PlayerEntity player, ItemStack food) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        SkillData data = SkillManager.get(serverPlayer);

        int before = data.getEnergy();
        int restored = calculateEnergyFromFood(food);
        if (restored <= 0) return;

        data.addEnergy(restored);

        if (data.getEnergy() != before) {
            SafeNetworkUtil.safeSend(
                    serverPlayer,
                    new EnergySyncPacket(data.getEnergy())
            );
        }
    }

    private static int calculateEnergyFromFood(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null) return 0;

        EnergyConfig cfg = EnergyConfig.get();

        int hunger = food.nutrition();
        float saturation = food.saturation();

        if (hunger >= 8 && saturation >= 0.8f) {
            return cfg.foodRestoreFeast;
        }

        if (hunger >= 6) {
            return cfg.foodRestoreMeal;
        }

        if (hunger >= 4) {
            return cfg.foodRestoreSimple;
        }

        return cfg.foodRestoreSnack;
    }
}

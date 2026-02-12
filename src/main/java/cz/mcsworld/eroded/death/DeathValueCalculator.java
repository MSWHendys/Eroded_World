package cz.mcsworld.eroded.death;

import net.minecraft.item.ItemStack;

import java.util.List;

public final class DeathValueCalculator {

    private DeathValueCalculator() {}

    public static long calculate(List<ItemStack> snapshot) {
        long value = 0;

        for (ItemStack stack : snapshot) {
            if (stack.isEmpty()) continue;
            value += stack.getCount();
        }

        return value;
    }
}

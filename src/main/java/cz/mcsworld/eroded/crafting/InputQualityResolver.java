package cz.mcsworld.eroded.crafting;

import net.minecraft.item.ItemStack;

import java.util.List;

public final class InputQualityResolver {

    private InputQualityResolver() {}

    public static float resolveAverage(List<ItemStack> inputs) {
        int count = 0;
        float sum = 0.0f;

        for (ItemStack stack : inputs) {
            Quality q = ItemQuality.get(stack);
            if (q != null) {
                sum += toNumeric(q);
                count++;
            }
        }

        if (count == 0) {
            return 1.0f;
        }

        return sum / count;
    }

    private static float toNumeric(Quality quality) {
        return switch (quality) {
            case POOR -> 0.0f;
            case STANDARD -> 1.0f;
            case EXCELLENT -> 2.0f;
        };
    }
}

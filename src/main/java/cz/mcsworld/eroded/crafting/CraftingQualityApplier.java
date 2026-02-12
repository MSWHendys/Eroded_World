package cz.mcsworld.eroded.crafting;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class CraftingQualityApplier {

    public static void apply(ItemStack stack, Quality quality) {

        if (stack.isEmpty()) return;

        ItemQuality.set(stack, quality);

        QualityEffectsApplier.apply(stack, quality);

        LoreComponent existing = stack.get(DataComponentTypes.LORE);
        List<Text> lines = new ArrayList<>();

        if (existing != null) {
            lines.addAll(existing.lines());
        }

        if (!lines.isEmpty()) {
            lines.add(Text.literal(""));
        }

        lines.add(
                Text.translatable(
                        "eroded.crafting.quality.line",
                        Text.translatable(getQualityKey(quality))
                )
        );

        stack.set(DataComponentTypes.LORE, new LoreComponent(lines));
    }

    private static String getQualityKey(Quality quality) {
        return switch (quality) {
            case POOR -> "eroded.crafting.quality.poor";
            case STANDARD -> "eroded.crafting.quality.standard";
            case EXCELLENT -> "eroded.crafting.quality.excellent";
        };
    }
}

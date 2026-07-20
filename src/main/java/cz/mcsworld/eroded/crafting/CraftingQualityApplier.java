package cz.mcsworld.eroded.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

public class CraftingQualityApplier {

    public static void apply(ItemStack stack, Quality quality) {

        if (stack.isEmpty()) return;

        ItemQuality.set(stack, quality);

        QualityEffectsApplier.apply(stack, quality);

        ItemLore existing = stack.get(DataComponents.LORE);
        List<Component> lines = new ArrayList<>();

        if (existing != null) {
            for (Component line : existing.lines()) {

                String str = line.getString();

                if (str.contains("Kvalita")
                        || str.contains("Quality")) {
                    continue;
                }

                lines.add(line);
            }
        }

        if (!lines.isEmpty()) {
            lines.add(Component.literal(""));
        }

        lines.add(
                Component.translatable(
                        "eroded.crafting.quality.line",
                        Component.translatable(getQualityKey(quality))
                )
        );

        stack.set(DataComponents.LORE, new ItemLore(lines));
    }

    private static String getQualityKey(Quality quality) {
        return switch (quality) {
            case POOR -> "eroded.crafting.quality.poor";
            case STANDARD -> "eroded.crafting.quality.standard";
            case EXCELLENT -> "eroded.crafting.quality.excellent";
        };
    }
}

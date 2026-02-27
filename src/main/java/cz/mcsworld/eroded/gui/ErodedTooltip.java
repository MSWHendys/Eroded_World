package cz.mcsworld.eroded.gui;

import cz.mcsworld.eroded.crafting.ItemQuality;
import cz.mcsworld.eroded.crafting.Quality;
import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ErodedTooltip {

    public static void register() {

        ItemTooltipCallback.EVENT.register(
                (stack, context, type, lines) -> {

                    if (stack.isEmpty()) return;

                    Quality quality = ItemQuality.get(stack);
                    if (quality == Quality.STANDARD) return;

                    var root = CraftingConfig.get();
                    var cfg = root.quality;

                    lines.add(Text.literal(""));

                    float durabilityMult = switch (quality) {
                        case POOR -> cfg.poorDurabilityMultiplier;
                        case STANDARD -> cfg.standardDurabilityMultiplier;
                        case EXCELLENT -> cfg.excellentDurabilityMultiplier;
                    };

                    float repairMult = switch (quality) {
                        case POOR -> cfg.poorRepairMultiplier;
                        case STANDARD -> cfg.standardRepairMultiplier;
                        case EXCELLENT -> cfg.excellentRepairMultiplier;
                    };

                    addMultiplierLine(
                            lines,
                            Text.translatable("eroded.tooltip.durability"),
                            durabilityMult
                    );

                    addMultiplierLine(
                            lines,
                            Text.translatable("eroded.tooltip.repair"),
                            repairMult
                    );
                }
        );
    }

    private static void addMultiplierLine(
            List<Text> lines,
            Text label,
            float mult
    ) {
        int percent = Math.round((mult - 1.0f) * 100.0f);
        if (percent == 0) return;

        Formatting fmt = (percent > 0) ? Formatting.GREEN : Formatting.RED;
        String sign = (percent > 0) ? "+" : "";

        lines.add(
                Text.translatable(
                        "eroded.tooltip.multiplier",
                        label,
                        sign + percent
                ).formatted(fmt)
        );
    }
}

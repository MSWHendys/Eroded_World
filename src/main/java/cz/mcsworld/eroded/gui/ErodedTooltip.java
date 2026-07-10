package cz.mcsworld.eroded.gui;

import cz.mcsworld.eroded.crafting.ItemQuality;
import cz.mcsworld.eroded.crafting.Quality;
import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

                    lines.add(Component.literal(""));

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
                            Component.translatable("eroded.tooltip.durability"),
                            durabilityMult
                    );

                    addMultiplierLine(
                            lines,
                            Component.translatable("eroded.tooltip.repair"),
                            repairMult
                    );
                }
        );
    }

    private static void addMultiplierLine(
            List<Component> lines,
            Component label,
            float mult
    ) {
        int percent = Math.round((mult - 1.0f) * 100.0f);
        if (percent == 0) return;

        ChatFormatting fmt = (percent > 0) ? ChatFormatting.GREEN : ChatFormatting.RED;
        String sign = (percent > 0) ? "+" : "";

        lines.add(
                Component.translatable(
                        "eroded.tooltip.multiplier",
                        label,
                        sign + percent
                ).withStyle(fmt)
        );
    }
}

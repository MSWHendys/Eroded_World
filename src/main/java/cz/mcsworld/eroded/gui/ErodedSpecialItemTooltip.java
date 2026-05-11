package cz.mcsworld.eroded.gui;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public final class ErodedSpecialItemTooltip {

    private ErodedSpecialItemTooltip() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack == null || stack.isEmpty()) {
                return;
            }

            if (stack.isOf(ErodedItems.ENERGY_DRINK)) {
                lines.add(Text.literal(""));
                lines.add(
                        Text.translatable("eroded.tooltip.energy_drink_effect")
                                .formatted(Formatting.GOLD)
                );
                return;
            }

            if (stack.isOf(ErodedItems.ADRENALINE_SHOT)) {
                int seconds = Math.max(
                        1,
                        EnergyConfig.get().server.adrenalineShot.immunitySeconds
                );

                lines.add(Text.literal(""));
                lines.add(
                        Text.translatable(
                                "eroded.tooltip.adrenaline_duration",
                                formatSeconds(seconds)
                        ).formatted(Formatting.GOLD)
                );
                return;
            }

            if (stack.isOf(ErodedBlocks.WARDING_LANTERN.asItem())) {
                int remainingSeconds = getLampDurationSeconds(stack);

                lines.add(Text.literal(""));
                lines.add(
                        Text.translatable(
                                "eroded.tooltip.lamp_duration",
                                formatSeconds(remainingSeconds)
                        ).formatted(Formatting.GOLD)
                );
            }
        });
    }

    private static int getLampDurationSeconds(ItemStack stack) {
        int configuredSeconds = Math.max(
                1,
                DarknessConfigs.get().server.wardingLamp.durationSeconds
        );

        int maxDamage = Math.max(1, stack.getMaxDamage());
        int damage = Math.max(0, stack.getDamage());

        float remainingRatio = 1.0f - (damage / (float) maxDamage);

        return Math.max(0, Math.round(configuredSeconds * remainingRatio));
    }

    private static String formatSeconds(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        }

        return seconds + " s";
    }
}
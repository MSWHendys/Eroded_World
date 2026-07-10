package cz.mcsworld.eroded.gui;

import cz.mcsworld.eroded.client.data.ErodedCompassClientData;
import cz.mcsworld.eroded.death.ErodedCompassItem;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class ErodedCompassTooltip {

    private ErodedCompassTooltip() {}

    private static long lastStableSeconds = -1;
    private static String cachedTime = null;

    private static long lastTargetLong = Long.MIN_VALUE;

    private static final long JITTER_SECONDS = 1;
    private static final long RESET_JUMP_SECONDS = 5;

    public static void register() {

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {

            if (stack.isEmpty()) return;
            if (!(stack.getItem() instanceof ErodedCompassItem)) return;

            if (!ErodedCompassClientData.isActive()) {
                resetCache();
                return;
            }

            long ticks = ErodedCompassClientData.getRemainingTicks();
            if (ticks <= 0) return;

            BlockPos target = ErodedCompassClientData.getTarget();
            if (target == null) return;

            long targetLong = target.asLong();

            if (targetLong != lastTargetLong) {
                resetCache();
                lastTargetLong = targetLong;
            }

            long seconds = ticks / 20;

            if (cachedTime == null || lastStableSeconds < 0) {
                setCached(seconds);
            } else {
                if (seconds < lastStableSeconds) {
                    setCached(seconds);
                } else {
                    long diff = seconds - lastStableSeconds;

                    if (diff <= JITTER_SECONDS) {
                    } else if (diff >= RESET_JUMP_SECONDS) {
                        setCached(seconds);
                    }
                }
            }

            if (cachedTime == null) return;

            lines.add(Component.literal(""));
            lines.add(Component.translatable(
                    "eroded.compass.tooltip.header"
            ).withStyle(ChatFormatting.GRAY));

            lines.add(Component.translatable(
                    "eroded.compass.tooltip.time",
                    cachedTime
            ).withStyle(ChatFormatting.GOLD));

            lines.add(Component.translatable(
                    "eroded.compass.tooltip.coords",
                    target.getX(),
                    target.getY(),
                    target.getZ()
            ).withStyle(ChatFormatting.DARK_GRAY));
        });
    }

    private static void setCached(long seconds) {
        lastStableSeconds = Math.max(0, seconds);

        long min = lastStableSeconds / 60;
        long sec = lastStableSeconds % 60;

        cachedTime = String.format("%02d:%02d", min, sec);
    }

    private static void resetCache() {
        lastStableSeconds = -1;
        cachedTime = null;
        lastTargetLong = Long.MIN_VALUE;
    }
}

package cz.mcsworld.eroded.client.hud;

import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

public class EnergyHud implements HudRenderCallback {

    private static final String ICON = "⚡";

    private static int lastEnergyValue = -1;
    private static boolean isRegenerating = false;

    private static long warningUntilMs = 0L;
    private static SkillData.EnergyState activeWarningState = null;

    private static final long POSITION_PREVIEW_DURATION_MS = 3000L;
    private static long positionPreviewUntilMs = 0L;

    public EnergyHud() {}

    public static void triggerWarning(SkillData.EnergyState state) {
        if (state == null || state == SkillData.EnergyState.NORMAL) return;

        activeWarningState = state;
        warningUntilMs = System.currentTimeMillis() + warningDurationMs(state);
    }

    public static void resetWarning() {
        warningUntilMs = 0L;
        activeWarningState = null;
    }

    /**
     * Temporarily keeps the Energy HUD visible after /eroded icon changes its position.
     * This lets players immediately see the selected position even at full Energy.
     */
    public static void showPositionPreview() {
        positionPreviewUntilMs = System.currentTimeMillis() + POSITION_PREVIEW_DURATION_MS;
    }

    private static long warningDurationMs(SkillData.EnergyState state) {
        var cfg = EnergyConfig.get().client.hud;
        return switch (state) {
            case TIRED -> cfg.tiredWarningDurationMs;
            case EXHAUSTED -> cfg.exhaustedWarningDurationMs;
            case EMPTY -> cfg.emptyWarningDurationMs;
            case NORMAL -> 0L;
        };
    }

    @Override
    public void onHudRender(GuiGraphics context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || !ClientEnergyData.isInitialized()) return;
        if (!ClientEnergyData.isEnabled()) return;

        var root = EnergyConfig.get();
        var cfg = root.client.hud;
        if (!cfg.energyHudEnabled) return;

        int energy = ClientEnergyData.getEnergy();
        int maxEnergy = ClientEnergyData.getMaxEnergy();
        boolean isImmune = ClientEnergyData.isImmune();
        int immunitySecs = ClientEnergyData.getImmunitySeconds();

        if (maxEnergy <= 0) maxEnergy = root.server.core.maxEnergy;

        if (energy > lastEnergyValue && lastEnergyValue != -1) {
            isRegenerating = true;
        } else if (energy < lastEnergyValue) {
            isRegenerating = false;
        }
        if (energy >= maxEnergy) {
            isRegenerating = false;
        }
        lastEnergyValue = energy;

        long nowMs = System.currentTimeMillis();
        boolean positionPreviewActive = positionPreviewUntilMs > nowMs;

        if (!cfg.showHudWhenFull
                && energy >= maxEnergy
                && !isImmune
                && !positionPreviewActive) return;

        int total = cfg.numberEnergyFlashes;
        int ticks = client.gui.getGuiTicks();

        int screenW = context.guiWidth();
        int screenH = context.guiHeight();
        int spacing = 8;
        int hudWidth = total * spacing;

        int margin = cfg.hudMargin;
        int posIconHUD_Y = cfg.posIconHUD_Y;
        int posTextHUD_Y = cfg.posTextHUD_Y;

        int x;
        int y;

        switch (cfg.hudPosition) {
            case CENTER_DOWN -> {
                x = (screenW - hudWidth) / 2;
                // Keep the centered HUD above the vanilla health/food rows and hotbar.
                // LEFT_DOWN / RIGHT_DOWN retain the configurable bottom offset.
                int centerDownOffset = Math.max(posIconHUD_Y, 52);
                y = screenH - centerDownOffset;
            }
            case LEFT_DOWN -> {
                x = margin;
                y = screenH - posIconHUD_Y;
            }
            case RIGHT_DOWN -> {
                x = screenW - hudWidth - margin;
                y = screenH - posIconHUD_Y;
            }
            case LEFT_UP -> {
                x = margin;
                y = margin;
            }
            case CENTER_UP -> {
                x = (screenW - hudWidth) / 2;
                y = margin;
            }
            case RIGHT_UP -> {
                x = screenW - hudWidth - margin;
                y = margin;
            }
            default -> {
                x = (screenW - hudWidth) / 2;
                y = screenH - posIconHUD_Y;
            }
        }

        boolean hideIconsBecauseChat = client.screen instanceof ChatScreen;
        if (!hideIconsBecauseChat) {
            for (int i = 0; i < total; i++) {
                int drawX = x + i * 8;
                EnergyHudLogic.SegmentVisual visual =
                        EnergyHudLogic.resolve(i, total, energy, maxEnergy, isRegenerating, ticks);

                if (!visual.visible()) {
                    context.drawString(client.font, ICON, drawX, y, EnergyHudLogic.EMPTY, true);
                    continue;
                }

                int iconColor = isImmune ? 0xFFFFD700 : visual.color();

                var matrices = context.pose();
                matrices.pushMatrix();
                float cx = drawX + 4;
                float cy = y + 4;
                matrices.translate(cx, cy);
                matrices.scale(visual.scale(), visual.scale());
                matrices.translate(-cx, -cy);

                context.drawString(
                        client.font,
                        ICON,
                        drawX,
                        y,
                        (0xFF << 24) | (iconColor & 0x00FFFFFF),
                        true
                );
                matrices.popMatrix();
            }

            if (isImmune) {
                int barWidth = hudWidth;
                int barHeight = 2;
                int barX = x;
                int barY = y + 10;
                float progress = Math.min(1.0f, immunitySecs / 120.0f);

                context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xAA000000);

                context.fill(barX, barY, barX + (int)(barWidth * progress), barY + barHeight, 0xFFFFD700);

            }
        }

        if (warningUntilMs > nowMs && !isRegenerating && root.server.warnings.warningsEnabled) {
            String key = (activeWarningState == null)
                    ? null
                    : EnergyHudLogic.getWarningTranslationKey(activeWarningState);

            if (key != null) {
                Component text = Component.translatable(key);
                int textWidth = client.font.width(text);
                int textX = (screenW - textWidth) / 2;
                int textY = y - posTextHUD_Y;
                int padding = 4;

                context.fill(textX - padding, textY - padding, textX + textWidth + padding, textY + client.font.lineHeight + padding, 0xCC000000);
                context.drawString(client.font, text, textX, textY, EnergyHudLogic.RED, true);

            } else {
                resetWarning();
            }
        } else if (activeWarningState != null && warningUntilMs <= nowMs) {
            resetWarning();
        }
    }
}

package cz.mcsworld.eroded.client.hud;

import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class EnergyHud {

    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("eroded", "energy_hud");

    private static final String ICON = "⚡";

    private static int lastEnergyValue = -1;
    private static boolean isRegenerating = false;

    private static long warningUntilMs = 0L;
    private static SkillData.EnergyState activeWarningState = null;

    private static final long POSITION_PREVIEW_DURATION_MS = 3000L;
    private static long positionPreviewUntilMs = 0L;

    private EnergyHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                ID,
                EnergyHud::render
        );
    }

    public static void triggerWarning(SkillData.EnergyState state) {
        if (state == null || state == SkillData.EnergyState.NORMAL) {
            return;
        }

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

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || !ClientEnergyData.isInitialized()) {
            return;
        }
        if (!ClientEnergyData.isEnabled()) {
            return;
        }

        var root = EnergyConfig.get();
        var cfg = root.client.hud;

        if (!cfg.energyHudEnabled) {
            return;
        }

        int energy = ClientEnergyData.getEnergy();
        int maxEnergy = ClientEnergyData.getMaxEnergy();
        boolean isImmune = ClientEnergyData.isImmune();
        int immunitySecs = ClientEnergyData.getImmunitySeconds();

        if (maxEnergy <= 0) {
            maxEnergy = root.server.core.maxEnergy;
        }

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
                && !positionPreviewActive) {
            return;
        }

        int total = cfg.numberEnergyFlashes;
        int ticks = client.gui.getGuiTicks();

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
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
                int drawX = x + i * spacing;

                EnergyHudLogic.SegmentVisual visual =
                        EnergyHudLogic.resolve(
                                i,
                                total,
                                energy,
                                maxEnergy,
                                isRegenerating,
                                ticks
                        );

                int iconColor;
                if (!visual.visible()) {
                    iconColor = EnergyHudLogic.EMPTY;
                } else {
                    iconColor = isImmune ? 0xFFFFD700 : visual.color();
                    iconColor = 0xFF000000 | (iconColor & 0x00FFFFFF);
                }

                var pose = graphics.pose();
                pose.pushMatrix();
                float cx = drawX + 4.0F;
                float cy = y + 4.0F;
                pose.translate(cx, cy);
                pose.scale(visual.scale(), visual.scale());
                pose.translate(-cx, -cy);

                graphics.text(
                        client.font,
                        ICON,
                        drawX,
                        y,
                        iconColor,
                        true
                );

                pose.popMatrix();
            }

            if (isImmune) {
                int barWidth = hudWidth;
                int barHeight = 2;
                int barX = x;
                int barY = y + 10;
                float progress = Math.min(1.0F, immunitySecs / 120.0F);

                graphics.fill(
                        barX,
                        barY,
                        barX + barWidth,
                        barY + barHeight,
                        0xAA000000
                );

                graphics.fill(
                        barX,
                        barY,
                        barX + (int) (barWidth * progress),
                        barY + barHeight,
                        0xFFFFD700
                );
            }
        }

        if (warningUntilMs > nowMs
                && !isRegenerating
                && root.server.warnings.warningsEnabled) {

            String key = activeWarningState == null
                    ? null
                    : EnergyHudLogic.getWarningTranslationKey(activeWarningState);

            if (key != null) {
                Component text = Component.translatable(key);
                int textWidth = client.font.width(text);
                int textX = (screenW - textWidth) / 2;
                int textY = y - posTextHUD_Y;
                int padding = 4;

                graphics.fill(
                        textX - padding,
                        textY - padding,
                        textX + textWidth + padding,
                        textY + client.font.lineHeight + padding,
                        0xCC000000
                );

                graphics.text(
                        client.font,
                        text,
                        textX,
                        textY,
                        EnergyHudLogic.RED,
                        true
                );
            } else {
                resetWarning();
            }
        } else if (activeWarningState != null && warningUntilMs <= nowMs) {
            resetWarning();
        }
    }
}

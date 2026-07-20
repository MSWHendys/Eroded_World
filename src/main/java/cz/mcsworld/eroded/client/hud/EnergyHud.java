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

    private static int warningTicks = 0;
    private static SkillData.EnergyState activeWarningState = null;

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
        warningTicks = EnergyConfig.get().client.hud.warningMessageTime;
    }

    public static void resetWarning() {
        warningTicks = 0;
        activeWarningState = null;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || !ClientEnergyData.isInitialized()) {
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

        if (!cfg.showHudWhenFull && energy >= maxEnergy && !isImmune) {
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

                float cx = drawX + 4.0f;
                float cy = y + 4.0f;
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

                float progress = Math.min(1.0f, immunitySecs / 120.0f);

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

        if (warningTicks > 0 && !isRegenerating && root.server.warnings.warningsEnabled) {
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

                warningTicks--;

                if (warningTicks <= 0) {
                    activeWarningState = null;
                }
            } else {
                warningTicks = 0;
                activeWarningState = null;
            }
        }
    }
}
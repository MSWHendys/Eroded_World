package cz.mcsworld.eroded.client.hud;

import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class EnergyHud implements HudRenderCallback {

    private static final String ICON = "⚡";

    private static int lastEnergyValue = -1;
    private static boolean isRegenerating = false;

    private static int warningTicks = 0;
    private static SkillData.EnergyState activeWarningState = null;

    public EnergyHud() {}

    public static void triggerWarning(SkillData.EnergyState state) {
        if (state == null || state == SkillData.EnergyState.NORMAL) return;

        activeWarningState = state;
        warningTicks = EnergyConfig.get().client.hud.warningMessageTime;
    }

    public static void resetWarning() {
        warningTicks = 0;
        activeWarningState = null;
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !ClientEnergyData.isInitialized()) return;

        var root = EnergyConfig.get();
        var cfg = root.client.hud;
        if (!cfg.energyHudEnabled) return;

        int energy = ClientEnergyData.getEnergy();
        int maxEnergy = ClientEnergyData.getMaxEnergy();
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

        if (!cfg.showHudWhenFull && energy >= maxEnergy) return;

        int total = cfg.numberEnergyFlashes;
        int ticks = client.inGameHud.getTicks();

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
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
                y = screenH - posIconHUD_Y;
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

        boolean hideIconsBecauseChat = client.currentScreen instanceof ChatScreen;
        if (!hideIconsBecauseChat) {
            for (int i = 0; i < total; i++) {
                int drawX = x + i * 8;
                EnergyHudLogic.SegmentVisual visual =
                        EnergyHudLogic.resolve(i, total, energy, maxEnergy, isRegenerating, ticks);

                if (!visual.visible()) {
                    context.drawText(client.textRenderer, ICON, drawX, y, EnergyHudLogic.EMPTY, true);
                    continue;
                }

                var matrices = context.getMatrices();
                matrices.pushMatrix();
                float cx = drawX + 4;
                float cy = y + 4;
                matrices.translate(cx, cy);
                matrices.scale(visual.scale(), visual.scale());
                matrices.translate(-cx, -cy);

                context.drawText(
                        client.textRenderer,
                        ICON,
                        drawX,
                        y,
                        (0xFF << 24) | (visual.color() & 0x00FFFFFF),
                        true
                );
                matrices.popMatrix();
            }
        }

        if (warningTicks > 0 && !isRegenerating && root.server.warnings.warningsEnabled) {

            String key = (activeWarningState == null)
                    ? null
                    : EnergyHudLogic.getWarningTranslationKey(activeWarningState);

            if (key != null) {
                Text text = Text.translatable(key);
                int textWidth = client.textRenderer.getWidth(text);

                int textX = (screenW - textWidth) / 2;
                int textY = y - posTextHUD_Y;

                int padding = 4;

                context.fill(
                        textX - padding,
                        textY - padding,
                        textX + textWidth + padding,
                        textY + client.textRenderer.fontHeight + padding,
                        0xCC000000
                );

                context.drawText(
                        client.textRenderer,
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
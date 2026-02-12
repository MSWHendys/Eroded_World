package cz.mcsworld.eroded.client.hud;

import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class EnergyHud implements HudRenderCallback {

    private static final String ICON = "⚡";

    private static int lastEnergyValue = -1;
    private static boolean isRegenerating = false;

    private static int warningTicks = 0;
    private static SkillData.EnergyState lastKnownState = SkillData.EnergyState.NORMAL;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !ClientEnergyData.isInitialized()) return;

        EnergyConfig cfg = EnergyConfig.get();
        if (!cfg.energyHudEnabled) return;

        int energy = ClientEnergyData.getEnergy();
        int maxEnergy = ClientEnergyData.getMaxEnergy();
        if (maxEnergy <= 0) maxEnergy = cfg.maxEnergy;

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

        SkillData.EnergyState currentState = EnergyHudLogic.getCurrentState(energy, maxEnergy);

        if (!isRegenerating && currentState != SkillData.EnergyState.NORMAL && currentState != lastKnownState) {
            warningTicks = cfg.WarningMessageTime;
        }
        lastKnownState = currentState;

        int total = cfg.numberEnergyFlashes;
        int ticks = client.inGameHud.getTicks();

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int x = (screenW / 2) - 91;
        int y = screenH - 49;

        for (int i = 0; i < total; i++) {
            int drawX = x + i * 8;
            EnergyHudLogic.SegmentVisual visual = EnergyHudLogic.resolve(i, total, energy, maxEnergy, isRegenerating, ticks);

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

            context.drawText(client.textRenderer, ICON, drawX, y, (0xFF << 24) | (visual.color() & 0x00FFFFFF), true);
            matrices.popMatrix();
        }

        if (warningTicks > 0 && !isRegenerating) {
            String key = EnergyHudLogic.getWarningTranslationKey(currentState);

            if (key != null) {
                Text text = Text.translatable(key);
                int textWidth = client.textRenderer.getWidth(text);

                int textX = (screenW - textWidth) / 2;
                int textY = y - 10;

                context.drawText(
                        client.textRenderer,
                        text,
                        textX,
                        textY,
                        EnergyHudLogic.RED,
                        true
                );
                warningTicks--;
            } else {
                warningTicks = 0;
            }
        }
    }
}
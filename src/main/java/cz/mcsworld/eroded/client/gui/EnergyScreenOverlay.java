package cz.mcsworld.eroded.client.gui;

import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.client.hud.EnergyHudLogic;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class EnergyScreenOverlay {

    private static final String ICON = "⚡";

    private static int lastEnergyValue = -1;
    private static boolean isRegenerating = false;

    private static int craftingFailTicks = 0;

    private static int warningTicks = 0;
    private static SkillData.EnergyState activeWarningState = null;

    private EnergyScreenOverlay() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
                ScreenEvents.afterRender(screen).register(EnergyScreenOverlay::render)
        );
    }

    public static void triggerWarning(SkillData.EnergyState state) {
        activeWarningState = state;
        warningTicks = EnergyConfig.get().client.hud.warningMessageTime;
    }

    public static void onCraftingFail() {
        craftingFailTicks = EnergyConfig.get().client.hud.warningMessageTime;
    }

    private static void render(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || !ClientEnergyData.isInitialized()) return;

        var root = EnergyConfig.get();
        var cfg = root.client.hud;

        if (!cfg.energyHudEnabled) return;

        int energy = ClientEnergyData.getEnergy();
        int maxEnergy = ClientEnergyData.getMaxEnergy();
        if (maxEnergy <= 0) maxEnergy = root.server.core.maxEnergy;

        if (energy > lastEnergyValue && lastEnergyValue != -1) isRegenerating = true;
        else if (energy < lastEnergyValue) isRegenerating = false;
        if (energy >= maxEnergy) isRegenerating = false;

        lastEnergyValue = energy;

        int total = cfg.numberEnergyFlashes;
        int ticks = client.inGameHud.getTicks();
        int screenW = context.getScaledWindowWidth();
        int barWidth = total * 8;
        int iconsX = (screenW - barWidth) / 2;
        int barCenterX = iconsX + barWidth / 2;
        int iconsY = 8;

        for (int i = 0; i < total; i++) {
            int drawX = iconsX + i * 8;

            EnergyHudLogic.SegmentVisual visual =
                    EnergyHudLogic.resolve(i, total, energy, maxEnergy, isRegenerating, ticks);

            if (!visual.visible()) {
                context.drawText(client.textRenderer, ICON, drawX, iconsY, EnergyHudLogic.EMPTY, true);
                continue;
            }

            var matrices = context.getMatrices();
            matrices.pushMatrix();
            float cx = drawX + 4;
            float cy = iconsY + 4;
            matrices.translate(cx, cy);
            matrices.scale(visual.scale(), visual.scale());
            matrices.translate(-cx, -cy);

            context.drawText(
                    client.textRenderer,
                    ICON,
                    drawX,
                    iconsY,
                    (0xFF << 24) | (visual.color() & 0x00FFFFFF),
                    true
            );

            matrices.popMatrix();
        }

        Text textToDraw = null;
        int textColor = 0xFFFFFFFF;

        if (craftingFailTicks > 0) {
            textToDraw = Text.translatable("eroded.crafting.not_enough_energy");
            textColor = EnergyHudLogic.RED;
            craftingFailTicks--;
        }

        else if (warningTicks > 0 && activeWarningState != null) {
            String key = EnergyHudLogic.getWarningTranslationKey(activeWarningState);
            if (key != null) {
                textToDraw = Text.translatable(key);
                textColor = EnergyHudLogic.RED;
                warningTicks--;
            } else {
                warningTicks = 0;
            }
        }

        else {
            textToDraw = Text.literal(energy + " / " + maxEnergy);
        }

        if (textToDraw != null) {
            int w = client.textRenderer.getWidth(textToDraw);
            context.drawText(
                    client.textRenderer,
                    textToDraw,
                    barCenterX - w / 2,
                    iconsY + 10,
                    textColor,
                    true
            );
        }
    }
}
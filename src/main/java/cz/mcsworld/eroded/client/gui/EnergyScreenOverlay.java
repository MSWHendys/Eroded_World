package cz.mcsworld.eroded.client.gui;

import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.client.hud.EnergyHudLogic;
import cz.mcsworld.eroded.client.screen.TerritoryModuleScreen;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EnergyScreenOverlay {

    private static final String ICON = "⚡";

    private static String anvilQuality = null;
    private static int lastEnergyValue = -1;
    private static boolean isRegenerating = false;

    private static int craftingFailTicks = 0;

    private static int warningTicks = 0;
    private static SkillData.EnergyState activeWarningState = null;

    private static int anvilMessageTicks = 0;
    private static Component anvilMessage = null;

    private static int customMessageTicks = 0;
    private static Component customMessage = null;
    private static int customMessageColor = 0xFFFFFFFF;

    private EnergyScreenOverlay() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
                ScreenEvents.afterExtract(screen).register(EnergyScreenOverlay::render)
        );
    }

    public static void triggerWarning(SkillData.EnergyState state) {
        activeWarningState = state;
        warningTicks = EnergyConfig.get().client.hud.warningMessageTime;
    }

    public static void onCraftingFail() {
        craftingFailTicks = EnergyConfig.get().client.hud.warningMessageTime;
    }

    public static void showAnvilMessage(Component text, String quality) {
        anvilMessage = text;
        anvilQuality = quality;
        anvilMessageTicks = EnergyConfig.get().client.hud.warningMessageTime * 5;
    }

    public static void showCustomMessage(Component text, int color) {
        customMessage = text;
        customMessageColor = color;
        customMessageTicks = EnergyConfig.get().client.hud.warningMessageTime * 2;
    }

    private static void render(
            Screen screen,
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        if (screen instanceof TerritoryModuleScreen) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (client == null || client.player == null || !ClientEnergyData.isInitialized()) {
            return;
        }

        var root = EnergyConfig.get();
        var cfg = root.client.hud;

        if (!cfg.energyHudEnabled) {
            return;
        }

        int energy = ClientEnergyData.getEnergy();
        int maxEnergy = ClientEnergyData.getMaxEnergy();

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

        int total = cfg.numberEnergyFlashes;
        int ticks = client.gui.hud.getGuiTicks();

        int screenW = client.getWindow().getGuiScaledWidth();

        int barWidth = total * 8;
        int iconsX = (screenW - barWidth) / 2;
        int barCenterX = iconsX + barWidth / 2;
        int iconsY = 8;

        for (int i = 0; i < total; i++) {
            int drawX = iconsX + i * 8;

            EnergyHudLogic.SegmentVisual visual =
                    EnergyHudLogic.resolve(i, total, energy, maxEnergy, isRegenerating, ticks);

            int color;

            if (!visual.visible()) {
                color = EnergyHudLogic.EMPTY;
            } else {
                color = 0xFF000000 | (visual.color() & 0x00FFFFFF);
            }


            var matrices = graphics.pose();;
            matrices.pushMatrix();
            float cx = drawX + 4;
            float cy = iconsY + 4;
            matrices.translate(cx, cy);
            matrices.scale(visual.scale(), visual.scale());
            matrices.translate(-cx, -cy);

            graphics.text(
                    client.font,
                    ICON,
                    drawX,
                    iconsY,
                    (0xFF << 24) | (visual.color() & 0x00FFFFFF),
                    true
            );


            matrices.popMatrix();
        }

        Component textToDraw = null;
        int textColor = 0xFFFFFFFF;

        if (anvilMessageTicks > 0 && anvilMessage != null) {
            textToDraw = anvilMessage;

            if (anvilQuality != null) {
                switch (anvilQuality) {
                    case "POOR" -> textColor = EnergyHudLogic.RED;
                    case "STANDARD" -> textColor = EnergyHudLogic.YELLOW;
                    case "EXCELLENT" -> textColor = EnergyHudLogic.GREEN;
                    default -> textColor = 0xFFFFFFFF;
                }
            } else {
                textColor = 0xFFFFFFFF;
            }

            anvilMessageTicks--;
        } else if (customMessageTicks > 0 && customMessage != null) {
            textToDraw = customMessage;
            textColor = customMessageColor;
            customMessageTicks--;

            if (customMessageTicks <= 0) {
                customMessage = null;
            }
        } else if (craftingFailTicks > 0) {
            textToDraw = Component.translatable("eroded.crafting.not_enough_energy");
            textColor = EnergyHudLogic.RED;
            craftingFailTicks--;
        } else if (warningTicks > 0 && activeWarningState != null) {
            String key = EnergyHudLogic.getWarningTranslationKey(activeWarningState);

            if (key != null) {
                textToDraw = Component.translatable(key);
                textColor = EnergyHudLogic.RED;
                warningTicks--;
            } else {
                warningTicks = 0;
            }
        } else {
            textToDraw = Component.translatable("eroded.gui.energy.prefix")
                    .append(Component.literal(energy + " / " + maxEnergy));
        }

        if (textToDraw != null) {
            int width = client.font.width(textToDraw);

            graphics.text(
                    client.font,
                    textToDraw,
                    barCenterX - width / 2,
                    iconsY + 10,
                    textColor,
                    true
            );
        }
    }
}
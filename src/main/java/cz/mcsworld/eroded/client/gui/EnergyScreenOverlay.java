package cz.mcsworld.eroded.client.gui;

import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.client.hud.EnergyHudLogic;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class EnergyScreenOverlay {

    private static final String ICON = "⚡";

    private static String anvilQuality = null;
    private static int lastEnergyValue = -1;
    private static boolean isRegenerating = false;

    private static int craftingFailTicks = 0;

    private static long warningUntilMs = 0L;
    private static SkillData.EnergyState activeWarningState = null;

    private static int anvilMessageTicks = 0;
    private static Component anvilMessage = null;

    private static int customMessageTicks = 0;
    private static Component customMessage = null;
    private static int customMessageColor = 0xFFFFFFFF;

    private EnergyScreenOverlay() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                ScreenEvents.afterExtract(screen).register(EnergyScreenOverlay::render)
        );
    }

    public static void triggerWarning(SkillData.EnergyState state) {
        if (state == null || state == SkillData.EnergyState.NORMAL) {
            return;
        }
        activeWarningState = state;
        warningUntilMs = System.currentTimeMillis() + warningDurationMs(state);
    }

    public static void onCraftingFail() {
        craftingFailTicks = EnergyConfig.get().client.hud.warningMessageTime;
    }

    public static void resetWarning() {
        warningUntilMs = 0L;
        activeWarningState = null;
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

    public static void resetEnergyState() {
        craftingFailTicks = 0;
        resetWarning();
        lastEnergyValue = -1;
        isRegenerating = false;
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

    private static boolean shouldRender(Screen screen) {
        return screen instanceof InventoryScreen
                || screen instanceof CraftingScreen
                || screen instanceof AnvilScreen;
    }

    private static void render(
            Screen screen,
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        if (!shouldRender(screen)) {
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

        boolean energyEnabled = ClientEnergyData.isEnabled();
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
        int ticks = client.gui.getGuiTicks();

        int screenWidth = client.getWindow().getGuiScaledWidth();

        int barWidth = total * 8;
        int iconsX = (screenWidth - barWidth) / 2;
        int barCenterX = iconsX + barWidth / 2;
        int iconsY = 8;

        if (energyEnabled) {
            for (int i = 0; i < total; i++) {
                int drawX = iconsX + i * 8;

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
                    iconColor = 0xFF000000 | (visual.color() & 0x00FFFFFF);
                }

                var pose = graphics.pose();
                pose.pushMatrix();
                float centerX = drawX + 4.0F;
                float centerY = iconsY + 4.0F;
                pose.translate(centerX, centerY);
                pose.scale(visual.scale(), visual.scale());
                pose.translate(-centerX, -centerY);

                graphics.text(
                        client.font,
                        ICON,
                        drawX,
                        iconsY,
                        iconColor,
                        true
                );

                pose.popMatrix();
            }
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
            }

            anvilMessageTicks--;
            if (anvilMessageTicks <= 0) {
                anvilMessage = null;
                anvilQuality = null;
            }

        } else if (customMessageTicks > 0 && customMessage != null) {
            textToDraw = customMessage;
            textColor = customMessageColor;
            customMessageTicks--;

            if (customMessageTicks <= 0) {
                customMessage = null;
                customMessageColor = 0xFFFFFFFF;
            }

        } else if (energyEnabled && craftingFailTicks > 0) {
            textToDraw = Component.translatable("eroded.crafting.not_enough_energy");
            textColor = EnergyHudLogic.RED;
            craftingFailTicks--;

        } else if (energyEnabled
                && activeWarningState != null
                && warningUntilMs > System.currentTimeMillis()) {

            String translationKey = EnergyHudLogic.getWarningTranslationKey(activeWarningState);
            if (translationKey != null) {
                textToDraw = Component.translatable(translationKey);
                textColor = EnergyHudLogic.RED;
            } else {
                resetWarning();
            }

        } else if (activeWarningState != null
                && warningUntilMs <= System.currentTimeMillis()) {
            resetWarning();
            if (energyEnabled) {
                textToDraw = Component.translatable("eroded.gui.energy.prefix")
                        .append(Component.literal(energy + " / " + maxEnergy));
            }

        } else if (energyEnabled) {
            textToDraw = Component.translatable("eroded.gui.energy.prefix")
                    .append(Component.literal(energy + " / " + maxEnergy));
        }

        if (textToDraw == null) {
            return;
        }

        int textWidth = client.font.width(textToDraw);
        graphics.text(
                client.font,
                textToDraw,
                barCenterX - textWidth / 2,
                iconsY + 10,
                textColor,
                true
        );
    }
}

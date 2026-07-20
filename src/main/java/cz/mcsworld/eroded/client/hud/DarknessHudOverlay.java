package cz.mcsworld.eroded.client.hud;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class DarknessHudOverlay {

    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("eroded", "darkness_overlay");

    private static float smoothAlpha = 0.0F;

    private DarknessHudOverlay() {}

    public static void register() {
        HudElementRegistry.addFirst(
                ID,
                DarknessHudOverlay::render
        );
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.level == null || client.player == null) {
            return;
        }

        if (client.options.hideGui) {
            return;
        }

        if (client.player.tickCount < 40) {
            return;
        }

        boolean guiOpen = client.screen != null;

        float targetAlpha;

        if (guiOpen) {
            targetAlpha = DarknessClientData.getFrozenEyeAlpha();
        } else {
            float eye = DarknessClientData.tickAndGetAlpha();
            float local = DarknessClientData.getLocalLightDarkness();
            float sky = DarknessClientData.getSkyLimiter();
            float block = DarknessClientData.getBlockLightLimiter();

            float base = eye + (local - eye) * local;
            targetAlpha = base * sky * block;
        }

        smoothAlpha = Mth.lerp(0.08F, smoothAlpha, targetAlpha);

        if (smoothAlpha <= 0.001F) {
            return;
        }

        var root = DarknessConfigs.get();

        if (!root.enabled) {
            return;
        }

        var cfg = root.client;

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        int maxAlpha = Math.max(0, Math.min(255, cfg.darknessMaxAlpha));
        int alpha = Math.round(maxAlpha * smoothAlpha);

        int color = alpha << 24;

        graphics.fill(
                0,
                0,
                width,
                height,
                color
        );

        if (cfg.darknessVignetteEnabled) {
            int vMax = Math.max(0, Math.min(255, cfg.darknessVignetteMaxAlpha));
            int vAlpha = Math.round(vMax * smoothAlpha);

            drawVignette(
                    graphics,
                    width,
                    height,
                    vAlpha,
                    cfg.darknessVignetteSize
            );
        }
    }

    private static void drawVignette(
            GuiGraphicsExtractor graphics,
            int width,
            int height,
            int baseAlpha,
            float size
    ) {
        int edge = Math.round(
                Math.min(width, height) * Math.max(0.05F, Math.min(0.30F, size))
        );

        int full = baseAlpha << 24;
        int clear = 0x00000000;

        graphics.fillGradient(
                0,
                0,
                width,
                edge,
                full,
                clear
        );

        graphics.fillGradient(
                0,
                height - edge,
                width,
                height,
                clear,
                full
        );

        graphics.fillGradient(
                0,
                edge,
                edge,
                height - edge,
                full,
                clear
        );

        graphics.fillGradient(
                width - edge,
                edge,
                width,
                height - edge,
                clear,
                full
        );
    }
}
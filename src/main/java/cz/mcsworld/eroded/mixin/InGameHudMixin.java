package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {

    @Unique
    private static float smoothAlpha = 0f;

    @Inject(
            method = "render",
            at = @At("HEAD")
    )
    private void eroded$renderDarknessUnderHud(
            GuiGraphics context,
            DeltaTracker tickCounter,
            CallbackInfo ci
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        if (client.player.tickCount < 40) return;

        boolean guiOpen = client.screen != null;

        float targetAlpha;
        if (guiOpen) {
            targetAlpha = DarknessClientData.getFrozenEyeAlpha();
        } else {
            float eye = DarknessClientData.tickAndGetAlpha();
            float local = DarknessClientData.getLocalLightDarkness();
            float sky   = DarknessClientData.getSkyLimiter();
            float block = DarknessClientData.getBlockLightLimiter();

            float base = eye + (local - eye) * local;
            targetAlpha = base * sky * block;
        }

        smoothAlpha = Mth.lerp(0.08f, smoothAlpha, targetAlpha);

        if (smoothAlpha <= 0.001f) return;

        var root = DarknessConfigs.get();
        if (!root.enabled) return;

        var cfg = root.client;


        int w = context.guiWidth();
        int h = context.guiHeight();

        int maxAlpha = Math.max(0, Math.min(255, cfg.darknessMaxAlpha));
        int alpha = Math.round(maxAlpha * smoothAlpha);

        int color = (alpha << 24);
        context.fill(0, 0, w, h, color);

        if (cfg.darknessVignetteEnabled) {
            int vMax = Math.max(0, Math.min(255, cfg.darknessVignetteMaxAlpha));
            int vAlpha = Math.round(vMax * smoothAlpha);
            drawVignette(context, w, h, vAlpha, cfg.darknessVignetteSize);
        }
    }

    @Unique
    private void drawVignette(
            GuiGraphics context,
            int w,
            int h,
            int baseAlpha,
            float size
    ) {
        int edge = Math.round(
                Math.min(w, h) * Math.max(0.05f, Math.min(0.30f, size))
        );

        int full = (baseAlpha << 24);
        int clear = 0x00000000;

        context.fillGradient(0, 0, w, edge, full, clear);
        context.fillGradient(0, h - edge, w, h, clear, full);
        context.fillGradient(0, edge, edge, h - edge, full, clear);
        context.fillGradient(w - edge, edge, w, h - edge, clear, full);
    }
}
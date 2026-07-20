package cz.mcsworld.eroded.visuals.darkness;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LightLayer;

public class DarknessDebugOverlay implements HudRenderCallback {
    @Override
    public void onHudRender(GuiGraphics context, net.minecraft.client.DeltaTracker tickCounter) {
        if (!DarknessClientData.SHOW_DEBUG_PANEL) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        var pos = client.player.blockPosition();

        int block = client.level.getBrightness(LightLayer.BLOCK, pos);
        int sky   = client.level.getBrightness(LightLayer.SKY, pos);

        float local = DarknessClientData.getLocalLightDarkness();
        float eye   = DarknessClientData.getEyeAlphaDebug();
        float finalAlpha = eye + (local - eye) * local;

        int x = 6;
        int y = 6;
        int color = 0xFFFFFFFF;

        context.drawString(
                client.font,
                Component.translatable("eroded.debug.darkness.title"),
                x, y, color, true
        );
        y += 10;

        context.drawString(
                client.font,
                Component.translatable("eroded.debug.darkness.light", block, sky),
                x, y, color, true
        );
        y += 10;

        context.drawString(
                client.font,
                Component.translatable("eroded.debug.darkness.local", String.format("%.2f", local)),
                x, y, color, true
        );
        y += 10;

        context.drawString(
                client.font,
                Component.translatable("eroded.debug.darkness.eye", String.format("%.2f", eye)),
                x, y, color, true
        );
        y += 10;

        context.drawString(
                client.font,
                Component.translatable("eroded.debug.darkness.final", String.format("%.2f", finalAlpha)),
                x, y, color, true
        );
    }
}

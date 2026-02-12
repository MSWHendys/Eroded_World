package cz.mcsworld.eroded.visuals.darkness;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.world.LightType;

public class DarknessDebugOverlay implements HudRenderCallback {
    @Override
    public void onHudRender(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        if (!DarknessClientData.SHOW_DEBUG_PANEL) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        var pos = client.player.getBlockPos();

        int block = client.world.getLightLevel(LightType.BLOCK, pos);
        int sky   = client.world.getLightLevel(LightType.SKY, pos);

        float local = DarknessClientData.getLocalLightDarkness();
        float eye   = DarknessClientData.getEyeAlphaDebug();
        float finalAlpha = eye + (local - eye) * local;

        int x = 6;
        int y = 6;
        int color = 0xFFFFFFFF;

        context.drawText(
                client.textRenderer,
                Text.translatable("eroded.debug.darkness.title"),
                x, y, color, true
        );
        y += 10;

        context.drawText(
                client.textRenderer,
                Text.translatable("eroded.debug.darkness.light", block, sky),
                x, y, color, true
        );
        y += 10;

        context.drawText(
                client.textRenderer,
                Text.translatable("eroded.debug.darkness.local", String.format("%.2f", local)),
                x, y, color, true
        );
        y += 10;

        context.drawText(
                client.textRenderer,
                Text.translatable("eroded.debug.darkness.eye", String.format("%.2f", eye)),
                x, y, color, true
        );
        y += 10;

        context.drawText(
                client.textRenderer,
                Text.translatable("eroded.debug.darkness.final", String.format("%.2f", finalAlpha)),
                x, y, color, true
        );
    }
}

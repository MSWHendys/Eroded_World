package cz.mcsworld.eroded.visuals.darkness;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LightLayer;

public final class DarknessDebugOverlay {

    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("eroded", "darkness_debug_overlay");

    private DarknessDebugOverlay() {
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                ID,
                DarknessDebugOverlay::render
        );
    }

    private static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        if (!DarknessClientData.SHOW_DEBUG_PANEL) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            return;
        }

        var pos = client.player.blockPosition();

        int block = client.level.getBrightness(LightLayer.BLOCK, pos);
        int sky = client.level.getBrightness(LightLayer.SKY, pos);

        float local = DarknessClientData.getLocalLightDarkness();
        float eye = DarknessClientData.getEyeAlphaDebug();
        float finalAlpha = eye + (local - eye) * local;

        int x = 6;
        int y = 6;
        int color = 0xFFFFFFFF;

        context.text(
                client.font,
                Component.translatable("eroded.debug.darkness.title"),
                x,
                y,
                color,
                true
        );
        y += 10;

        context.text(
                client.font,
                Component.translatable("eroded.debug.darkness.light", block, sky),
                x,
                y,
                color,
                true
        );
        y += 10;

        context.text(
                client.font,
                Component.translatable("eroded.debug.darkness.local", String.format("%.2f", local)),
                x,
                y,
                color,
                true
        );
        y += 10;

        context.text(
                client.font,
                Component.translatable("eroded.debug.darkness.eye", String.format("%.2f", eye)),
                x,
                y,
                color,
                true
        );
        y += 10;

        context.text(
                client.font,
                Component.translatable("eroded.debug.darkness.final", String.format("%.2f", finalAlpha)),
                x,
                y,
                color,
                true
        );
    }
}
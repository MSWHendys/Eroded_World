package cz.mcsworld.eroded.client.ui;

import cz.mcsworld.eroded.client.gui.EnergyScreenOverlay;
import cz.mcsworld.eroded.client.hud.EnergyHud;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.network.EnergyWarningPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

public final class EnergyWarningClientHandler {

    private static long lastShown = 0L;

    private EnergyWarningClientHandler() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                EnergyWarningPacket.ID,
                (payload, context) -> context.client().execute(() -> {

                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null) return;

                    var root = EnergyConfig.get();
                    var cfg = root.server.warnings;
                    if (!cfg.warningsEnabled) return;

                    long now = System.currentTimeMillis();
                    long cooldown = cfg.warningCooldownMs;

                    if (now - lastShown < cooldown) return;
                    lastShown = now;

                    client.player.playSound(
                            SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                            0.6f,
                            0.6f
                    );

                    EnergyHud.triggerWarning(payload.state());
                    EnergyScreenOverlay.triggerWarning(payload.state());
                })
        );
    }
}
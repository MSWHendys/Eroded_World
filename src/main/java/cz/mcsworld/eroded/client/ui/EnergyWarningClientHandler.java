package cz.mcsworld.eroded.client.ui;

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
                (payload, context) -> {

                    long now = System.currentTimeMillis();
                    long cooldown =
                            EnergyConfig.get().warningCooldownMs;

                    if (now - lastShown < cooldown) return;
                    lastShown = now;

                    MinecraftClient client = MinecraftClient.getInstance();

                    client.execute(() -> {
                        if (client.player == null) return;
                        client.player.playSound(
                                SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                                0.6f,
                                0.6f
                        );
                    });
                }
        );
    }
}

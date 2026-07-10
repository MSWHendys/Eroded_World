package cz.mcsworld.eroded.client.audio;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;

public final class HeartbeatClient {

    private static int cooldown = 0;

    private HeartbeatClient() {}

    public static void tick() {

        var root = DarknessConfigs.get();
        if (!root.enabled) return;

        var cfg = root.client;
        if (!cfg.heartbeatEnabled) return;

        float volumeMul = Mth.clamp(cfg.audio.volumeMultiplier, 0.1f, 1.0f);
        float delayMul  = Mth.clamp(cfg.audio.delayMultiplier, 0.5f, 2.0f);

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        boolean dark = DarknessClientData.isDarknessActive();

        if (!dark) {
            cooldown = 0;
            return;
        }

        if (cooldown-- > 0) return;



        BlockPos eyePos = BlockPos.containing(
                client.player.getX(),
                client.player.getEyeY(),
                client.player.getZ()
        );

        int sky = client.level.getBrightness(LightLayer.SKY, eyePos);
        if (sky >= cfg.skySafeThreshold) return;

        client.getSoundManager().play(
                SimpleSoundInstance.forUI(
                        SoundEvents.WARDEN_HEARTBEAT,
                        cfg.heartbeatVolume * volumeMul,
                        cfg.heartbeatPitch
                )
        );

        float intensity = DarknessClientData.getLocalLightDarkness();

        int minDelay = 40;
        int maxDelay = 120;

        int baseDelay = (int)(maxDelay - (maxDelay - minDelay) * intensity);

        cooldown = (int)(baseDelay * delayMul);
    }
}
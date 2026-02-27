package cz.mcsworld.eroded.client.audio;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;

public final class HeartbeatClient {

    private static int cooldown = 0;

    private HeartbeatClient() {}

    public static void tick() {

        var root = DarknessConfigs.get();
        if (!root.enabled) return;

        var cfg = root.client;
        if (!cfg.heartbeatEnabled) return;

        float volumeMul = MathHelper.clamp(cfg.audio.volumeMultiplier, 0.1f, 1.0f);
        float delayMul  = MathHelper.clamp(cfg.audio.delayMultiplier, 0.5f, 2.0f);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        boolean dark = DarknessClientData.isDarknessActive();

        // Pokud už nejsme v darkness → reset
        if (!dark) {
            cooldown = 0;
            return;
        }

        if (cooldown-- > 0) return;



        BlockPos eyePos = BlockPos.ofFloored(
                client.player.getX(),
                client.player.getEyeY(),
                client.player.getZ()
        );

        int sky = client.world.getLightLevel(LightType.SKY, eyePos);
        if (sky >= cfg.skySafeThreshold) return;

        client.getSoundManager().play(
                PositionedSoundInstance.master(
                        SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                        cfg.heartbeatVolume * volumeMul,
                        cfg.heartbeatPitch
                )
        );

        // === Dynamická rychlost podle intenzity ===
        float intensity = DarknessClientData.getLocalLightDarkness(); // 0–1

        int minDelay = 40;
        int maxDelay = 120;

        int baseDelay = (int)(maxDelay - (maxDelay - minDelay) * intensity);

        cooldown = (int)(baseDelay * delayMul);
    }
}
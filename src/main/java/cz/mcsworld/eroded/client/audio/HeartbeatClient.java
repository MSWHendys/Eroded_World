package cz.mcsworld.eroded.client.audio;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

public final class HeartbeatClient {

    private HeartbeatClient() {}

    public static void tick() {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        if (!DarknessClientData.consumeDarknessEnter(
                DarknessClientData.isDarknessActive())) {
            return;
        }

        var cfg = DarknessConfigs.get().heartbeat;

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
                        cfg.volume,
                        cfg.pitch
                )
        );
    }
}

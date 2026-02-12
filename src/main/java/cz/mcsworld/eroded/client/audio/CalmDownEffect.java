package cz.mcsworld.eroded.client.audio;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public final class CalmDownEffect {

    private static int fadeTicksLeft = 0;
    private static boolean active = false;

    private CalmDownEffect() {}

    public static void trigger() {

        DarknessConfigs cfg = DarknessConfigs.get();
        DarknessConfigs.CalmDown calm = cfg.calmDown;

        fadeTicksLeft = calm.fadeTicks;
        active = true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.getSoundManager().play(
                PositionedSoundInstance.master(
                        SoundEvents.ENTITY_PLAYER_BREATH,
                        calm.volume,
                        calm.pitchMin + Random.create().nextFloat() * calm.pitchRand
                )
        );
    }

    public static float applyVisualFade(float baseAlpha) {

        if (!active) return baseAlpha;

        fadeTicksLeft--;
        if (fadeTicksLeft <= 0) {
            active = false;
            return baseAlpha;
        }

        float t = fadeTicksLeft / (float) DarknessConfigs.get().calmDown.fadeTicks;
        float factor = MathHelper.clamp(t * t, 0.0f, 1.0f);

        return baseAlpha * factor;
    }
}

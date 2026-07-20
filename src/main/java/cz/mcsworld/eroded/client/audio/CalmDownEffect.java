package cz.mcsworld.eroded.client.audio;

import cz.mcsworld.eroded.client.data.DarknessClientData;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class CalmDownEffect {

    private static int fadeTicksLeft = 0;
    private static int totalFadeTicks = 0;
    private static boolean active = false;

    private CalmDownEffect() {}

    public static void trigger() {

        var root = DarknessConfigs.get();
        if (!root.enabled) return;

        var client = root.client;
        if (!client.visualDarknessEnabled) return;
        if (!client.calmDown.enabled) return;

        var calm = client.calmDown;

        fadeTicksLeft = calm.fadeTicks;
        totalFadeTicks = calm.fadeTicks;
        active = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.getSoundManager().play(
                SimpleSoundInstance.forUI(
                        SoundEvents.PLAYER_BREATH,
                        calm.volume,
                        calm.pitchMin + RandomSource.create().nextFloat() * calm.pitchRand
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

        float t = fadeTicksLeft / (float) totalFadeTicks;

        float factor = Mth.clamp(t * t, 0.0f, 1.0f);

        return baseAlpha * factor;
    }

}
package cz.mcsworld.eroded.client.data;

import cz.mcsworld.eroded.config.ErodedConfig;
import cz.mcsworld.eroded.config.ErodedConfigs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;


public final class DarknessClientData {

    private static boolean targetDarkness = false;
    private static float smoothedLocalDarkness = 0.0f;
    private static float smoothedEyeTarget = 0.0f;
    private static boolean darknessLatched = false;
    private static int lightGraceTicks = 0;

    private static float alpha = 0.0f;

    private static boolean hasServerState = false;
    private DarknessClientData() {}
    private static int darknessStableTicks = 0;
    private static final int ENTER_STABLE_TICKS = 20;

    private static boolean inDarkness;
    private static boolean wasInDarkness = false;
    public static boolean consumeDarknessEnter(boolean current) {
        boolean entered = !wasInDarkness && current;
        wasInDarkness = current;
        return entered;
    }

    public static boolean SHOW_DEBUG_PANEL = false;
    public static void update(boolean value) {

        hasServerState = true;

        wasInDarkness = inDarkness;

        inDarkness = value;
        targetDarkness = value;
    }

    public static boolean consumeDarknessExit() {
        boolean exited = wasInDarkness && !inDarkness;
        wasInDarkness = inDarkness;
        return exited;
    }

    public static float tickAndGetAlpha() {

        ErodedConfig cfg = ErodedConfigs.get();

        float target = (hasServerState && targetDarkness) ? 1.0f : 0.0f;

        float targetSmoothing = 0.03f;

        smoothedEyeTarget +=
                (target - smoothedEyeTarget) * targetSmoothing;

        float speed = Math.max(0.001f, cfg.darknessFadeSpeed);

        if (alpha < smoothedEyeTarget) {
            alpha = Math.min(smoothedEyeTarget, alpha + speed);
        } else if (alpha > smoothedEyeTarget) {
            alpha = Math.max(smoothedEyeTarget, alpha - speed);
        }

        return alpha;
    }

    public static float getLocalLightDarkness() {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return smoothedLocalDarkness;

        var world = client.world;
        var player = client.player;

        BlockPos basePos = player.getBlockPos();
        var look = player.getRotationVec(1.0f);

        float totalBlockLight = 0.0f;
        int samples = 6;

        for (int i = 0; i < samples; i++) {
            float dist = 1.5f + i * 1.8f;

            BlockPos p = basePos.add(
                    MathHelper.floor(look.x * dist),
                    MathHelper.floor(look.y * dist),
                    MathHelper.floor(look.z * dist)
            );

            totalBlockLight += world.getLightLevel(LightType.BLOCK, p);
        }

        float avgBlock = totalBlockLight / samples;


        if (avgBlock >= 12f) {
            smoothedLocalDarkness += (0f - smoothedLocalDarkness) * 0.4f;
            return smoothedLocalDarkness;
        }

        float blockDarkness =
                1.0f - MathHelper.clamp((avgBlock - 2f) / 7f, 0f, 1f);

        blockDarkness = (float) Math.pow(blockDarkness, 1.3f);

        blockDarkness = (float) Math.pow(blockDarkness, 1.3f);

        float smoothing = 0.02f;
        smoothedLocalDarkness +=
                (blockDarkness - smoothedLocalDarkness) * smoothing;

        return smoothedLocalDarkness;
    }


    public static float getSkyLimiter() {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return 1.0f;

        var world = client.world;
        var player = client.player;
        BlockPos pos = player.getBlockPos();

        int sky = world.getLightLevel(LightType.SKY, pos);

        float limiter = 1.0f - MathHelper.clamp((sky - 2f) / 11f, 0f, 1f);

        return (float) Math.pow(limiter, 1.4f);
    }


    public static float getBlockLightLimiter() {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return 1.0f;

        var world = client.world;
        var player = client.player;
        BlockPos pos = player.getBlockPos();

        int block = world.getLightLevel(LightType.BLOCK, pos);

        float limiter = 1.0f - MathHelper.clamp((block - 2f) / 10f, 0f, 1f);

        return (float) Math.pow(limiter, 1.3f);
    }


    public static float getEyeAlphaDebug() {
        return alpha;
    }

    public static float getSmoothedLocalDarknessDebug() {
        return smoothedLocalDarkness;
    }

    public static boolean isDarknessActive() {

        if (!hasServerState) {
            darknessStableTicks = 0;
            return false;
        }

        float local = getLocalLightDarkness();

        if (local >= 0.6f) {
            darknessStableTicks++;

            if (darknessStableTicks >= ENTER_STABLE_TICKS) {
                darknessLatched = true;
                lightGraceTicks = 0;
                return true;
            }

            return false;
        } else {
            darknessStableTicks = 0;
        }

        if (darknessLatched) {

            lightGraceTicks++;

            if (lightGraceTicks < 80) {
                return true;
            }

            darknessLatched = false;
            return false;
        }

        return false;
    }

    public static float getFrozenEyeAlpha() {
        return alpha;
    }
}

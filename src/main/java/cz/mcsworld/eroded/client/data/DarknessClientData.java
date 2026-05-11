package cz.mcsworld.eroded.client.data;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.minecraft.client.MinecraftClient;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;

public final class DarknessClientData {
    private static float smoothedLocalDarkness = 0.0f;
    private static float smoothedEyeTarget = 0.0f;
    private static float alpha = 0.0f;
    private static boolean hasServerState = false;
    private static boolean serverInDarkness = false;
    private static int darknessStableTicks = 0;
    private static final int ENTER_STABLE_TICKS = 20;
    private static boolean darknessLatched = false;
    private static int lightGraceTicks = 0;
    private static boolean wasInDarkness = false;

    public static boolean SHOW_DEBUG_PANEL = false;

    private DarknessClientData() {}

    public static void update(boolean value) {
        hasServerState = true;
        wasInDarkness = serverInDarkness;
        serverInDarkness = value;
    }

    public static boolean isServerInDarkness() {
        return hasServerState && serverInDarkness;
    }

    public static boolean consumeDarknessExit() {
        boolean exited = wasInDarkness && !serverInDarkness;
        wasInDarkness = serverInDarkness;
        return exited;
    }

    public static float tickAndGetAlpha() {
        var root = DarknessConfigs.get();
        if (!root.enabled || !root.client.visualDarknessEnabled) return 0.0f;

        float target = isServerInDarkness() ? 1.0f : 0.0f;

        if (isHoldingWardingLantern()) {

            target = 0.0f;
        }

        float targetSmoothing = root.client.eyeSmoothing;
        smoothedEyeTarget += (target - smoothedEyeTarget) * targetSmoothing;

        float speed = Math.max(0.001f, root.client.fadeSpeed);
        if (alpha < smoothedEyeTarget) alpha = Math.min(smoothedEyeTarget, alpha + speed);
        else if (alpha > smoothedEyeTarget) alpha = Math.max(smoothedEyeTarget, alpha - speed);

        return alpha;
    }

    public static void updateLighLevel(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        var root = DarknessConfigs.get();
        if (!root.enabled) { smoothedLocalDarkness = 0.0f; return; }

        var world = client.world;
        var player = client.player;
        var cfg = root.client;

        BlockPos eyePos = player.getBlockPos().up(1);

        float areaLight = (float) world.getLightLevel(LightType.BLOCK, eyePos);

        var look = player.getRotationVec(1.0f);
        float totalLookLight = 0.0f;
        int samples = cfg.samples;
        int validSamples = 0;

        for (int i = 0; i < samples; i++) {
            float dist = (float) (cfg.sampleStart + i * cfg.sampleStep);
            BlockPos p = eyePos.add(
                    MathHelper.floor(look.x * dist),
                    MathHelper.floor(look.y * dist),
                    MathHelper.floor(look.z * dist)
            );

            if (!world.getBlockState(p).isFullCube(world, p)) {
                totalLookLight += world.getLightLevel(LightType.BLOCK, p);
                validSamples++;
            }
        }

        float avgLookLight = validSamples > 0 ? totalLookLight / validSamples : areaLight;
        float finalAvgLight = Math.max(avgLookLight, areaLight);

        if (isHoldingWardingLantern()) {
            finalAvgLight = Math.max(finalAvgLight, 8.0f);
        }

        if (finalAvgLight >= 12f) {
            smoothedLocalDarkness += (0f - smoothedLocalDarkness) * 0.4f;
            return;
        }

        float blockDarkness = 1.0f - MathHelper.clamp((finalAvgLight - 2f) / 7f, 0f, 1f);
        blockDarkness = (float) Math.pow(blockDarkness, cfg.blockCurve);
        smoothedLocalDarkness += (blockDarkness - smoothedLocalDarkness) * 0.1f;  //cfg.localSmoothing;
    }

    public static float getSkyLimiter() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return 1.0f;
        int sky = client.world.getLightLevel(LightType.SKY, client.player.getBlockPos());

        return 1.0f - MathHelper.clamp((sky - 8f) / 7f, 0f, 1f);
    }

    public static float getBlockLightLimiter() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return 1.0f;
        int block = client.world.getLightLevel(LightType.BLOCK, client.player.getBlockPos());

        float limiter = 1.0f - MathHelper.clamp((block - 2f) / 10f, 0f, 1f);
        return (float) Math.pow(limiter, 1.3f);
    }

    public static boolean isHoldingWardingLantern() {
        var client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return false;

        var player = client.player;
        return player.getMainHandStack().isOf(ErodedBlocks.WARDING_LANTERN.asItem()) ||
                player.getOffHandStack().isOf(ErodedBlocks.WARDING_LANTERN.asItem());
    }

    public static float getLocalLightDarkness() { return smoothedLocalDarkness; }
    public static float getEyeAlphaDebug() { return alpha; }
    public static float getSmoothedLocalDarknessDebug() { return smoothedLocalDarkness; }
    public static float getFrozenEyeAlpha() { return alpha; }

    public static boolean isDarknessActive() {
        var root = DarknessConfigs.get();
        if (!root.enabled || !root.client.visualDarknessEnabled || !hasServerState) return false;

        if (serverInDarkness) {
            darknessStableTicks++;
            if (darknessStableTicks >= ENTER_STABLE_TICKS) {
                darknessLatched = true;
                lightGraceTicks = 0;
                return true;
            }
        } else {
            darknessStableTicks = 0;
        }

        if (darknessLatched) {
            lightGraceTicks++;
            if (lightGraceTicks < root.client.graceTicks) return true;
            darknessLatched = false;
        }
        return false;
    }
}
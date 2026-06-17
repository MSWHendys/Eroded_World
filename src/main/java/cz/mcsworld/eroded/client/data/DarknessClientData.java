package cz.mcsworld.eroded.client.data;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;

public final class DarknessClientData {

    private static float smoothedLocalDarkness = 0.0F;
    private static float smoothedEyeTarget = 0.0F;
    private static float alpha = 0.0F;

    private static boolean hasServerState = false;
    private static boolean serverInDarkness = false;
    private static boolean wasInDarkness = false;

    private static int darknessStableTicks = 0;
    private static final int ENTER_STABLE_TICKS = 20;

    private static boolean darknessLatched = false;
    private static int lightGraceTicks = 0;

    public static boolean SHOW_DEBUG_PANEL = false;

    private static int compassDarknessBreakTicks = 0;
    private static float compassDarknessBreakMax = 1.0F;

    private DarknessClientData() {
    }

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

        if (!root.enabled || !root.client.visualDarknessEnabled) {
            return 0.0F;
        }

        float target = isServerInDarkness() ? 1.0F : 0.0F;

        if (isHoldingWardingLantern()) {
            target = 0.0F;
        }

        float targetSmoothing = MathHelper.clamp(
                root.client.eyeSmoothing,
                0.001F,
                1.0F
        );

        smoothedEyeTarget +=
                (target - smoothedEyeTarget) * targetSmoothing;

        float speed = MathHelper.clamp(
                root.client.fadeSpeed,
                0.001F,
                1.0F
        );

        if (alpha < smoothedEyeTarget) {
            alpha = Math.min(smoothedEyeTarget, alpha + speed);
        } else if (alpha > smoothedEyeTarget) {
            alpha = Math.max(smoothedEyeTarget, alpha - speed);
        }

        return applyAllDarknessBreaks(alpha);
    }

    public static void updateLightLevel(MinecraftClient client) {
        tickCompassDarknessBreak();

        if (client.world == null || client.player == null) {
            return;
        }

        var root = DarknessConfigs.get();

        if (!root.enabled) {
            smoothedLocalDarkness = 0.0F;
            return;
        }

        var world = client.world;
        var player = client.player;
        var cfg = root.client;

        BlockPos eyePos = player.getBlockPos().up(1);

        float areaLight = world.getLightLevel(
                LightType.BLOCK,
                eyePos
        );

        var look = player.getRotationVec(1.0F);

        float totalLookLight = 0.0F;
        int samples = Math.max(1, cfg.samples);
        int validSamples = 0;

        for (int i = 0; i < samples; i++) {
            float distance = (float) (
                    cfg.sampleStart + i * cfg.sampleStep
            );

            BlockPos samplePos = eyePos.add(
                    MathHelper.floor(look.x * distance),
                    MathHelper.floor(look.y * distance),
                    MathHelper.floor(look.z * distance)
            );

            if (!world.getBlockState(samplePos)
                    .isFullCube(world, samplePos)) {

                totalLookLight += world.getLightLevel(
                        LightType.BLOCK,
                        samplePos
                );

                validSamples++;
            }
        }

        float averageLookLight = validSamples > 0
                ? totalLookLight / validSamples
                : areaLight;

        float finalAverageLight = Math.max(
                averageLookLight,
                areaLight
        );

        if (isHoldingWardingLantern()) {
            finalAverageLight = Math.max(
                    finalAverageLight,
                    8.0F
            );
        }


        if (finalAverageLight >= 12.0F) {
            float clearSpeed = MathHelper.clamp(
                    cfg.darknessFadeSpeed,
                    0.001F,
                    1.0F
            );

            smoothedLocalDarkness +=
                    (0.0F - smoothedLocalDarkness) * clearSpeed;

            return;
        }

        float blockDarkness = 1.0F - MathHelper.clamp(
                (finalAverageLight - 2.0F) / 7.0F,
                0.0F,
                1.0F
        );

        blockDarkness = (float) Math.pow(
                blockDarkness,
                cfg.blockCurve
        );

        float localSmoothing = MathHelper.clamp(
                cfg.localSmoothing,
                0.001F,
                1.0F
        );

        smoothedLocalDarkness +=
                (blockDarkness - smoothedLocalDarkness)
                        * localSmoothing;
    }

    public static float getSkyLimiter() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null || client.player == null) {
            return 1.0F;
        }

        var cfg = DarknessConfigs.get().client;

        int skyLight = client.world.getLightLevel(
                LightType.SKY,
                client.player.getBlockPos()
        );

        float limiter = 1.0F - MathHelper.clamp(
                (skyLight - 8.0F) / 7.0F,
                0.0F,
                1.0F
        );

        return (float) Math.pow(limiter, cfg.skyCurve);
    }

    public static float getBlockLightLimiter() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null || client.player == null) {
            return 1.0F;
        }

        var cfg = DarknessConfigs.get().client;

        int blockLight = client.world.getLightLevel(
                LightType.BLOCK,
                client.player.getBlockPos()
        );

        float limiter = 1.0F - MathHelper.clamp(
                (blockLight - 2.0F) / 10.0F,
                0.0F,
                1.0F
        );

        return (float) Math.pow(limiter, cfg.blockCurve);
    }

    public static boolean isHoldingWardingLantern() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return false;
        }

        var player = client.player;

        return player.getMainHandStack().isOf(
                ErodedBlocks.WARDING_LANTERN.asItem()
        ) || player.getOffHandStack().isOf(
                ErodedBlocks.WARDING_LANTERN.asItem()
        );
    }

    public static float getLocalLightDarkness() {
        return applyAllDarknessBreaks(smoothedLocalDarkness);
    }

    public static float getEyeAlphaDebug() {
        return alpha;
    }

    public static float getSmoothedLocalDarknessDebug() {
        return smoothedLocalDarkness;
    }

    public static float getFrozenEyeAlpha() {
        return applyAllDarknessBreaks(alpha);
    }

    public static boolean isDarknessActive() {
        var root = DarknessConfigs.get();

        if (!root.enabled
                || !root.client.visualDarknessEnabled
                || !hasServerState) {
            return false;
        }

        float activationValue = Math.max(
                applyAllDarknessBreaks(alpha),
                applyAllDarknessBreaks(smoothedLocalDarkness)
        );


        if (isAnyDarknessBreakActive()
                && activationValue < root.client.enterThreshold) {

            darknessStableTicks = 0;
            darknessLatched = false;
            lightGraceTicks = 0;

            return false;
        }

        if (serverInDarkness
                && activationValue >= root.client.enterThreshold) {

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

            if (lightGraceTicks < root.client.graceTicks) {
                return true;
            }

            darknessLatched = false;
        }

        return false;
    }

    public static void activateCompassDarknessBreak(
            int ticks,
            float maxDarkness
    ) {
        int safeTicks = Math.max(0, ticks);

        float safeMaxDarkness = MathHelper.clamp(
                maxDarkness,
                0.0F,
                1.0F
        );

        compassDarknessBreakTicks = Math.max(
                compassDarknessBreakTicks,
                safeTicks
        );

        compassDarknessBreakMax = safeMaxDarkness;
    }

    private static void tickCompassDarknessBreak() {
        if (compassDarknessBreakTicks <= 0) {
            compassDarknessBreakMax = 1.0F;
            return;
        }

        compassDarknessBreakTicks--;

        if (compassDarknessBreakTicks <= 0) {
            compassDarknessBreakMax = 1.0F;
        }
    }

    private static boolean isAnyDarknessBreakActive() {
        return compassDarknessBreakTicks > 0;
    }

    private static float applyAllDarknessBreaks(float value) {
        return applyCompassDarknessBreak(value);
    }

    private static float applyCompassDarknessBreak(float value) {
        if (compassDarknessBreakTicks <= 0) {
            return value;
        }

        return Math.min(value, compassDarknessBreakMax);
    }
}
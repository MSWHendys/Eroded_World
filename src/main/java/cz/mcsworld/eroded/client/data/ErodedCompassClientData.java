package cz.mcsworld.eroded.client.data;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class ErodedCompassClientData {

    private static boolean active;
    private static BlockPos deathPos;

    private static float angle;

    private static long remainingTicks;

    private ErodedCompassClientData() {}

    public static void updateTarget(
            boolean active,
            BlockPos pos,
            long ticks
    ) {
        ErodedCompassClientData.active = active;
        ErodedCompassClientData.deathPos = pos;
        ErodedCompassClientData.remainingTicks = Math.max(0, ticks);
    }

    public static boolean isActive() {
        return active && deathPos != null && remainingTicks > 0;
    }

    public static BlockPos getTarget() {
        return isActive() ? deathPos : null;
    }

    public static float getAngle() {
        return angle;
    }

    public static void setAngle(float value) {
        angle = Mth.positiveModulo(value, 1.0F);
    }

    public static long getRemainingTicks() {
        return remainingTicks;
    }

    public static void tickTime() {
        if (!active) return;

        if (remainingTicks > 0) {
            remainingTicks--;
        }

        if (remainingTicks <= 0) {
            remainingTicks = 0;
            active = false;
            deathPos = null;
            angle = 0.0F;
        }
    }
}
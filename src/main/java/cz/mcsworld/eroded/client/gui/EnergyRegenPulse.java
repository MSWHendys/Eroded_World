package cz.mcsworld.eroded.client.gui;

public final class EnergyRegenPulse {

    private static final float SPEED = 0.15f;

    private static int pulsingIndex = -1;
    private static float phase = 0f;
    private static int lastFilled = -1;

    private EnergyRegenPulse() {}

    public static void update(int filledSegments, boolean isRegenerating) {

        if (!isRegenerating) {
            reset();
            lastFilled = filledSegments;
            return;
        }

        if (filledSegments > lastFilled) {
            pulsingIndex = filledSegments;
            phase = 0f;
        }

        if (pulsingIndex == -1) {
            pulsingIndex = filledSegments;
        }

        phase += SPEED;
        lastFilled = filledSegments;
    }

    public static boolean isPulsing(int index) {
        return index == pulsingIndex;
    }

    public static float getPulseStrength() {
        return (float) (0.5f + 0.5f * Math.sin(phase));
    }


    private static void reset() {
        pulsingIndex = -1;
        phase = 0f;
    }
}

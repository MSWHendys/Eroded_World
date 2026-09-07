package cz.mcsworld.eroded.energy;

/** Shared server/client rule for Energy-based block-breaking speed. */
public final class MiningSpeedRules {

    private MiningSpeedRules() {}

    public static float multiplier(
            boolean energyEnabled,
            boolean scalingEnabled,
            boolean allowMiningAtZero,
            int energy,
            int maxEnergy,
            int fullSpeedFromPercent,
            int reducedSpeedFromPercent,
            int reducedSpeedPercent,
            int criticalSpeedPercent
    ) {
        if (!energyEnabled || !scalingEnabled || maxEnergy <= 0) {
            return 1.0f;
        }

        if (energy <= 0) {
            return allowMiningAtZero ? clampPercent(criticalSpeedPercent) : 0.0f;
        }

        float percent = energy * 100.0f / maxEnergy;
        if (percent >= fullSpeedFromPercent) {
            return 1.0f;
        }
        if (percent >= reducedSpeedFromPercent) {
            return clampPercent(reducedSpeedPercent);
        }
        return clampPercent(criticalSpeedPercent);
    }

    private static float clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent)) / 100.0f;
    }
}

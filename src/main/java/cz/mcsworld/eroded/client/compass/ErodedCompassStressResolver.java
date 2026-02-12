package cz.mcsworld.eroded.client.compass;

import cz.mcsworld.eroded.config.death.DeathConfig;

public final class ErodedCompassStressResolver {

    private ErodedCompassStressResolver() {}

    public static float resolve(double distance) {

        var cfg = DeathConfig.get().compass.stress;

        if (distance < 0)
            return 0.0f;

        if (distance <= cfg.minDistance)
            return 1.0f;

        if (distance >= cfg.maxDistance)
            return 0.0f;

        double norm =
                1.0 - (distance - cfg.minDistance)
                        / (cfg.maxDistance - cfg.minDistance);

        return (float) clamp(norm, 0.0, 1.0);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}

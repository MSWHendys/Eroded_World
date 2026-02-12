package cz.mcsworld.eroded.client.compass;
import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.client.audio.HeartbeatClient;

public final class ErodedCompassHeartbeat {

    private static int timer = 0;

    private ErodedCompassHeartbeat() {}

    public static void clientTick() {

        double distance =
                ErodedCompassDistanceResolver.getDistanceToDeath();

        float stress =
                ErodedCompassStressResolver.resolve(distance);

        if (stress <= 0.05f) {
            timer = 0;
            return;
        }

        var cfg = DeathConfig.get().compass.heartbeat;

        int interval = lerp(
                cfg.maxInterval,
                cfg.minInterval,
                stress
        );

        if (timer > 0) {
            timer--;
            return;
        }

        HeartbeatClient.tick();
        timer = interval;
    }

    private static int lerp(int max, int min, float t) {
        return (int) (max - (max - min) * clamp(t));
    }

    private static float clamp(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}

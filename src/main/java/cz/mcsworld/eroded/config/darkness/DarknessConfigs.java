package cz.mcsworld.eroded.config.darkness;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Client-side konfigurace tmy, strachu a psychologických efektů.
 */
@Config(name = "ErodedWorld/darkness")
public class DarknessConfigs implements ConfigData {
    public static DarknessConfigs get() {
        return AutoConfig
                .getConfigHolder(DarknessConfigs.class)
                .getConfig();
    }

    @ConfigEntry.Gui.Tooltip
    public float darknessFadeSpeed = 0.015f;

    @ConfigEntry.Gui.Tooltip
    public float eyeTargetSmoothing = 0.03f;

    @ConfigEntry.Gui.CollapsibleObject
    public LocalDarkness localDarkness = new LocalDarkness();

    public static class LocalDarkness {

        @ConfigEntry.Gui.Tooltip
        public int samples = 6;

        @ConfigEntry.Gui.Tooltip
        public double sampleStart = 1.5;

        @ConfigEntry.Gui.Tooltip
        public double sampleStep = 1.8;

        @ConfigEntry.Gui.Tooltip
        public double blockCurve = 1.3;

        @ConfigEntry.Gui.Tooltip
        public double skyCurve = 1.4;

        @ConfigEntry.Gui.Tooltip
        public float smoothing = 0.025f;
    }

    @ConfigEntry.Gui.CollapsibleObject
    public DarknessLatch darknessLatch = new DarknessLatch();

    public static class DarknessLatch {

        @ConfigEntry.Gui.Tooltip
        public float enterThreshold = 0.6f;

        @ConfigEntry.Gui.Tooltip
        public int lightGraceTicks = 80;
    }

    @ConfigEntry.Gui.CollapsibleObject
    public Heartbeat heartbeat = new Heartbeat();

    public static class Heartbeat {

        @ConfigEntry.Gui.Tooltip
        public int skySafeThreshold = 10;

        @ConfigEntry.Gui.Tooltip
        public int ambientSafeThreshold = 8;

        @ConfigEntry.Gui.Tooltip
        public float volume = 0.8f;

        @ConfigEntry.Gui.Tooltip
        public float pitch = 1.0f;
    }

    @ConfigEntry.Gui.CollapsibleObject
    public CalmDown calmDown = new CalmDown();

    public static class CalmDown {

        @ConfigEntry.Gui.Tooltip
        public int fadeTicks = 40;

        @ConfigEntry.Gui.Tooltip
        public float volume = 0.6f;

        @ConfigEntry.Gui.Tooltip
        public float pitchMin = 0.9f;

        @ConfigEntry.Gui.Tooltip
        public float pitchRand = 0.2f;
    }
}

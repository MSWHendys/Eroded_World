package cz.mcsworld.eroded.config.darkness;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "ErodedWorld/darkness")
public class DarknessConfigs implements ConfigData {
    public static DarknessConfigs get() {
        return AutoConfig
                .getConfigHolder(DarknessConfigs.class)
                .getConfig();
    }

    // =========================================================
    // GLOBAL
    // =========================================================

    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Gui.CollapsibleObject
    public Server server = new Server();

    @ConfigEntry.Gui.CollapsibleObject
    public Client client = new Client();


    // =========================================================
    // SERVER SECTION (AUTORITA)
    // =========================================================
    public static class Server {

        @ConfigEntry.Gui.Tooltip
        public boolean mobLightFearEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public int fearLightThreshold = 4;            // FEAR_LIGHT_THRESHOLD

        @ConfigEntry.Gui.Tooltip
        public int suppressLightThreshold = 7;        // SUPPRESS_LIGHT_THRESHOLD

        @ConfigEntry.Gui.Tooltip
        public int lightSearchRadius = 8;             // MAX_RADIUS

        @ConfigEntry.Gui.Tooltip
        public int postLightCooldownTicks = 12;       // DarknessMobLightMemory

        @ConfigEntry.Gui.Tooltip
        public int flickerStages = 3;                 // DarknessFlickerState.MAX_STAGE


        // ---------- Light Eater ----------

        @ConfigEntry.Gui.Tooltip
        public boolean lightEaterEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public int lightEaterCheckInterval = 40;      // CHECK_INTERVAL

        @ConfigEntry.Gui.Tooltip
        public int lightEaterRadius = 2;              // RADIUS

        @ConfigEntry.Gui.Tooltip
        public int maxLightActionsPerTick = 6;        // MAX_ACTIONS_PER_TICK

        @ConfigEntry.Gui.Tooltip
        public float threatRequired = 0.6f;           // hardcoded 0.6f


        // ---------- AI ----------

        @ConfigEntry.Gui.Tooltip
        public double escapeSpeed = 1.2;

        @ConfigEntry.Gui.Tooltip
        public int escapeDistance = 5;
    }


    // =========================================================
    // CLIENT SECTION (VIZUAL + AUDIO)
    // =========================================================
    public static class Client {

        @ConfigEntry.Gui.Tooltip
        public boolean visualDarknessEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public float fadeSpeed = 0.015f;

        @ConfigEntry.Gui.Tooltip
        public float eyeSmoothing = 0.03f;

        @ConfigEntry.Gui.Tooltip
        public float enterThreshold = 0.6f;

        @ConfigEntry.Gui.Tooltip
        public int graceTicks = 80;


        // ---------- Local Sampling ----------

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
        public float localSmoothing = 0.02f;


        // ---------- Heartbeat ----------

        @ConfigEntry.Gui.Tooltip
        public boolean heartbeatEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public int skySafeThreshold = 10;

        @ConfigEntry.Gui.Tooltip
        public float heartbeatVolume = 0.8f;

        @ConfigEntry.Gui.Tooltip
        public float heartbeatPitch = 1.0f;

        @ConfigEntry.Gui.CollapsibleObject
        public CalmDown calmDown = new CalmDown();

        public static class CalmDown {

            @ConfigEntry.Gui.Tooltip
            public boolean enabled = true;

            @ConfigEntry.Gui.Tooltip
            public int fadeTicks = 40;

            @ConfigEntry.Gui.Tooltip
            public float volume = 0.6f;

            @ConfigEntry.Gui.Tooltip
            public float pitchMin = 0.9f;

            @ConfigEntry.Gui.Tooltip
            public float pitchRand = 0.2f;


        }

        @ConfigEntry.Gui.Tooltip
        public int darknessMaxAlpha = 220;

        @ConfigEntry.Gui.Tooltip
        public float darknessFadeSpeed = 0.08f;

        @ConfigEntry.Gui.Tooltip
        public boolean darknessVignetteEnabled = false;

        @ConfigEntry.Gui.Tooltip
        public int darknessVignetteMaxAlpha = 90;

        @ConfigEntry.Gui.Tooltip
        public float darknessVignetteSize = 0.18f;

        @ConfigEntry.Gui.CollapsibleObject
        public AudioTuning audio = new AudioTuning();

        public static class AudioTuning {

            @ConfigEntry.Gui.Tooltip
            public float volumeMultiplier = 1.0f; // 0.2 - 1.0

            @ConfigEntry.Gui.Tooltip
            public float delayMultiplier = 1.0f;  // 0.5 - 2.0
        }
    }

}
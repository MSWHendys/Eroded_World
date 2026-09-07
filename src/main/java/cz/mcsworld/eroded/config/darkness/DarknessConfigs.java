package cz.mcsworld.eroded.config.darkness;

import cz.mcsworld.eroded.config.ConfigValidation;
import cz.mcsworld.eroded.config.ConfigValidationException;
import cz.mcsworld.eroded.config.ErodedConfig;
import cz.mcsworld.eroded.config.ErodedConfigs;

public class DarknessConfigs implements ErodedConfig {

    public boolean enabled = true;

    public Server server = new Server();

    public Client client = new Client();

    public static DarknessConfigs get() {
        return ErodedConfigs.DARKNESS;
    }

    @Override
    public void validatePostLoad() throws ConfigValidationException {
        ConfigValidation.notNull(server, "darkness.server");
        ConfigValidation.notNull(client, "darkness.client");
        ConfigValidation.notNull(server.wardingLamp, "darkness.server.wardingLamp");
        ConfigValidation.notNull(server.erodedTorch, "darkness.server.erodedTorch");
        ConfigValidation.notNull(client.calmDown, "darkness.client.calmDown");
        ConfigValidation.notNull(client.audio, "darkness.client.audio");

        ConfigValidation.range(server.fearLightThreshold, 0, 15, "darkness.server.fearLightThreshold");
        ConfigValidation.range(server.suppressLightThreshold, 0, 15, "darkness.server.suppressLightThreshold");
        ConfigValidation.range(server.lightSearchRadius, 0, 32, "darkness.server.lightSearchRadius");
        ConfigValidation.min(server.postLightCooldownTicks, 0, "darkness.server.postLightCooldownTicks");
        ConfigValidation.min(server.flickerStages, 1, "darkness.server.flickerStages");
        ConfigValidation.require(server.escapeSpeed > 0.0 && Double.isFinite(server.escapeSpeed),
                "darkness.server.escapeSpeed", "must be > 0 and finite");
        ConfigValidation.range(server.escapeDistance, 0, 64, "darkness.server.escapeDistance");
        ConfigValidation.min(server.lightEaterCheckInterval, 1, "darkness.server.lightEaterCheckInterval");
        ConfigValidation.range(server.lightEaterRadius, 0, 16, "darkness.server.lightEaterRadius");
        ConfigValidation.range(server.maxLightActionsPerTick, 0, 128, "darkness.server.maxLightActionsPerTick");
        ConfigValidation.range(server.threatRequired, 0.0f, 1.0f, "darkness.server.threatRequired");

        ConfigValidation.min(server.wardingLamp.durationSeconds, 0, "darkness.server.wardingLamp.durationSeconds");
        ConfigValidation.range(server.wardingLamp.skyLightMax, 0, 15, "darkness.server.wardingLamp.skyLightMax");
        ConfigValidation.range(server.wardingLamp.lightLevel, 0, 15, "darkness.server.wardingLamp.lightLevel");

        ConfigValidation.min(server.erodedTorch.maxChargeTicks, 1, "darkness.server.erodedTorch.maxChargeTicks");
        ConfigValidation.min(server.erodedTorch.rechargeIntervalTicks, 1, "darkness.server.erodedTorch.rechargeIntervalTicks");
        ConfigValidation.min(server.erodedTorch.rechargeAmount, 0, "darkness.server.erodedTorch.rechargeAmount");
        ConfigValidation.range(server.erodedTorch.placedLightLevel, 0, 15, "darkness.server.erodedTorch.placedLightLevel");
        ConfigValidation.range(server.erodedTorch.activeSkyLightMax, 0, 15, "darkness.server.erodedTorch.activeSkyLightMax");
        ConfigValidation.range(server.erodedTorch.activeNightStartTime, 0, 23999, "darkness.server.erodedTorch.activeNightStartTime");
        ConfigValidation.range(server.erodedTorch.activeNightEndTime, 0, 23999, "darkness.server.erodedTorch.activeNightEndTime");

        ConfigValidation.require(client.fadeSpeed >= 0.0f && Float.isFinite(client.fadeSpeed), "darkness.client.fadeSpeed", "must be >= 0 and finite");
        ConfigValidation.require(client.eyeSmoothing >= 0.0f && Float.isFinite(client.eyeSmoothing), "darkness.client.eyeSmoothing", "must be >= 0 and finite");
        ConfigValidation.range(client.enterThreshold, 0.0f, 1.0f, "darkness.client.enterThreshold");
        ConfigValidation.min(client.graceTicks, 0, "darkness.client.graceTicks");
        ConfigValidation.range(client.darknessMaxAlpha, 0, 255, "darkness.client.darknessMaxAlpha");
        ConfigValidation.require(client.darknessFadeSpeed >= 0.0f && Float.isFinite(client.darknessFadeSpeed), "darkness.client.darknessFadeSpeed", "must be >= 0 and finite");
        ConfigValidation.range(client.darknessVignetteMaxAlpha, 0, 255, "darkness.client.darknessVignetteMaxAlpha");
        ConfigValidation.range(client.darknessVignetteSize, 0.0f, 1.0f, "darkness.client.darknessVignetteSize");
        ConfigValidation.range(client.samples, 1, 64, "darkness.client.samples");
        ConfigValidation.min(client.sampleStart, 0.0, "darkness.client.sampleStart");
        ConfigValidation.require(client.sampleStep > 0.0 && Double.isFinite(client.sampleStep), "darkness.client.sampleStep", "must be > 0 and finite");
        ConfigValidation.require(client.blockCurve > 0.0 && Double.isFinite(client.blockCurve), "darkness.client.blockCurve", "must be > 0 and finite");
        ConfigValidation.require(client.skyCurve > 0.0 && Double.isFinite(client.skyCurve), "darkness.client.skyCurve", "must be > 0 and finite");
        ConfigValidation.require(client.localSmoothing >= 0.0f && Float.isFinite(client.localSmoothing), "darkness.client.localSmoothing", "must be >= 0 and finite");
        ConfigValidation.range(client.skySafeThreshold, 0, 15, "darkness.client.skySafeThreshold");
        ConfigValidation.min(client.heartbeatVolume, 0.0f, "darkness.client.heartbeatVolume");
        ConfigValidation.require(client.heartbeatPitch > 0.0f && Float.isFinite(client.heartbeatPitch), "darkness.client.heartbeatPitch", "must be > 0 and finite");
        ConfigValidation.min(client.calmDown.fadeTicks, 0, "darkness.client.calmDown.fadeTicks");
        ConfigValidation.min(client.calmDown.volume, 0.0f, "darkness.client.calmDown.volume");
        ConfigValidation.require(client.calmDown.pitchMin > 0.0f && Float.isFinite(client.calmDown.pitchMin), "darkness.client.calmDown.pitchMin", "must be > 0 and finite");
        ConfigValidation.min(client.calmDown.pitchRand, 0.0f, "darkness.client.calmDown.pitchRand");
        ConfigValidation.min(client.audio.volumeMultiplier, 0.0f, "darkness.client.audio.volumeMultiplier");
        ConfigValidation.min(client.audio.delayMultiplier, 0.0f, "darkness.client.audio.delayMultiplier");
    }

    public static class Server {

        public boolean mobLightFearEnabled = true;

        public int fearLightThreshold = 4;

        public int suppressLightThreshold = 7;

        public int lightSearchRadius = 8;

        public int postLightCooldownTicks = 12;

        public int flickerStages = 3;

        public double escapeSpeed = 1.2;

        public int escapeDistance = 5;

        public boolean lightEaterEnabled = true;

        public int lightEaterCheckInterval = 40;

        public int lightEaterRadius = 2;

        public int maxLightActionsPerTick = 6;

        public float threatRequired = 0.6f;

        public WardingLamp wardingLamp = new WardingLamp();

        public ErodedTorch erodedTorch = new ErodedTorch();

        public static class WardingLamp {

            public int durationSeconds = 120;

            public int skyLightMax = 7;

            public int undergroundY = 60;

            public int lightLevel = 15;
        }

        public static class ErodedTorch {

            public boolean enabled = true;

            public int maxChargeTicks = 2400;

            public int rechargeIntervalTicks = 20;

            public int rechargeAmount = 1;

            public boolean drainOnlyInDarkness = true;

            public int placedLightLevel = 15;

            public int activeSkyLightMax = 7;

            public int activeNightStartTime = 12000;

            public int activeNightEndTime = 23000;

            public boolean rechargeHeldWhenInactive = true;
        }
    }

    public static class Client {

        public boolean visualDarknessEnabled = true;

        public float fadeSpeed = 0.015f;

        public float eyeSmoothing = 0.03f;

        public float enterThreshold = 0.6f;

        public int graceTicks = 80;

        public int darknessMaxAlpha = 220;

        public float darknessFadeSpeed = 0.08f;

        public boolean darknessVignetteEnabled = false;

        public int darknessVignetteMaxAlpha = 90;

        public float darknessVignetteSize = 0.18f;

        public int samples = 6;

        public double sampleStart = 1.5;

        public double sampleStep = 1.8;

        public double blockCurve = 1.3;

        public double skyCurve = 1.4;

        public float localSmoothing = 0.02f;

        public boolean heartbeatEnabled = true;

        public int skySafeThreshold = 10;

        public float heartbeatVolume = 0.8f;

        public float heartbeatPitch = 1.0f;

        public CalmDown calmDown = new CalmDown();

        public AudioTuning audio = new AudioTuning();

        public boolean showTorchChargeHud = true;

        public static class CalmDown {

            public boolean enabled = true;

            public int fadeTicks = 40;

            public float volume = 0.6f;

            public float pitchMin = 0.9f;

            public float pitchRand = 0.2f;
        }

        public static class AudioTuning {

            public float volumeMultiplier = 1.0f;

            public float delayMultiplier = 1.0f;
        }
    }
}

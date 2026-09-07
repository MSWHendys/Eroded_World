package cz.mcsworld.eroded.config.territory;

import cz.mcsworld.eroded.config.ConfigValidation;
import cz.mcsworld.eroded.config.ConfigValidationException;
import cz.mcsworld.eroded.config.ErodedConfig;
import cz.mcsworld.eroded.config.ErodedConfigs;

public class TerritoryConfig implements ErodedConfig {

    public Server server = new Server();

    public static TerritoryConfig get() {
        return ErodedConfigs.TERRITORY;
    }

    @Override
    public void validatePostLoad() throws ConfigValidationException {
        ConfigValidation.notNull(server, "territory.server");
        ConfigValidation.min(server.miningWeight, 0.0f, "territory.server.miningWeight");
        ConfigValidation.min(server.pollutionWeight, 0.0f, "territory.server.pollutionWeight");
        ConfigValidation.min(server.forestWeight, 0.0f, "territory.server.forestWeight");
        ConfigValidation.min(server.miningThreshold, 0, "territory.server.miningThreshold");
        ConfigValidation.min(server.collapseCooldownMs, 0L, "territory.server.collapseCooldownMs");
        ConfigValidation.range(server.stabilizerRadius, 0, 32, "territory.server.stabilizerRadius");
        ConfigValidation.range(server.collapseChanceLow, 0.0f, 1.0f, "territory.server.collapseChanceLow");
        ConfigValidation.range(server.collapseChanceMid, 0.0f, 1.0f, "territory.server.collapseChanceMid");
        ConfigValidation.range(server.collapseChanceHigh, 0.0f, 1.0f, "territory.server.collapseChanceHigh");
        ConfigValidation.range(server.collapseMobSpawnChance, 0.0f, 1.0f, "territory.server.collapseMobSpawnChance");
        ConfigValidation.min(server.ecosystemIntervalTicks, 1, "territory.server.ecosystemIntervalTicks");
        ConfigValidation.range(server.ecosystemVisibleRadiusBlocks, 0, 256, "territory.server.ecosystemVisibleRadiusBlocks");
        ConfigValidation.range(server.ecosystemMaxPlayersPerSlice, 1, 128, "territory.server.ecosystemMaxPlayersPerSlice");
        ConfigValidation.range(server.ecosystemAttemptsPerPlayer, 0, 128, "territory.server.ecosystemAttemptsPerPlayer");
        ConfigValidation.range(server.ecosystemSurfaceAttempts, 0, 128, "territory.server.ecosystemSurfaceAttempts");
        ConfigValidation.range(server.ecosystemLeafAttempts, 0, 128, "territory.server.ecosystemLeafAttempts");
        ConfigValidation.require(server.ecosystemLeafMaxY >= server.ecosystemLeafMinY, "territory.server.ecosystemLeafMaxY", "must be >= ecosystemLeafMinY");
        ConfigValidation.min(server.ecosystemCalmDownDelay, 0, "territory.server.ecosystemCalmDownDelay");
        ConfigValidation.min(server.statePruneIntervalTicks, 20, "territory.server.statePruneIntervalTicks");
        ConfigValidation.min(server.stateRetentionTicks, 1200L, "territory.server.stateRetentionTicks");
        ConfigValidation.range(server.ecosystemDegradeThreatThreshold, 0.0f, 1.0f, "territory.server.ecosystemDegradeThreatThreshold");
        ConfigValidation.range(server.ecosystemRegenThreatThreshold, 0.0f, 1.0f, "territory.server.ecosystemRegenThreatThreshold");
        ConfigValidation.range(server.grassDegradeChance, 0.0f, 1.0f, "territory.server.grassDegradeChance");
        ConfigValidation.range(server.grassRegrowChance, 0.0f, 1.0f, "territory.server.grassRegrowChance");
        ConfigValidation.range(server.permanentScarChance, 0.0f, 1.0f, "territory.server.permanentScarChance");
        ConfigValidation.min(server.ecosystemLeafLossMultiplier, 0.0f, "territory.server.ecosystemLeafLossMultiplier");
        ConfigValidation.range(server.ecosystemLeafLossMaxChance, 0.0f, 1.0f, "territory.server.ecosystemLeafLossMaxChance");
        ConfigValidation.range(server.ecosystemLeafLossMinChance, 0.0f, 1.0f, "territory.server.ecosystemLeafLossMinChance");
        ConfigValidation.require(server.ecosystemLeafLossMaxChance >= server.ecosystemLeafLossMinChance, "territory.server.ecosystemLeafLossMaxChance", "must be >= ecosystemLeafLossMinChance");
        ConfigValidation.min(server.undergroundTolerance, 0, "territory.server.undergroundTolerance");
        ConfigValidation.min(server.spawnCheckInterval, 1, "territory.server.spawnCheckInterval");
        ConfigValidation.range(server.spawnAttempts, 0, 128, "territory.server.spawnAttempts");
        ConfigValidation.range(server.spawnMinDistance, 0.0, 256.0, "territory.server.spawnMinDistance");
        ConfigValidation.require(server.spawnMaxDistance >= server.spawnMinDistance
                        && server.spawnMaxDistance <= 256.0
                        && Double.isFinite(server.spawnMaxDistance),
                "territory.server.spawnMaxDistance", "must be finite, >= spawnMinDistance and <= 256");
        ConfigValidation.range(server.spawnKeepMinChance, 0.0f, 1.0f, "territory.server.spawnKeepMinChance");
        ConfigValidation.range(server.mobMaxPerChunk, 0, 128, "territory.server.mobMaxPerChunk");
        ConfigValidation.range(server.maxMobsPerSpawnCycle, 0, 32, "territory.server.maxMobsPerSpawnCycle");
        ConfigValidation.range(server.mobDespawnRadius, 1, 512, "territory.server.mobDespawnRadius");
        ConfigValidation.require(
                server.mobDespawnRadius >= server.spawnMaxDistance,
                "territory.server.mobDespawnRadius",
                "must be >= spawnMaxDistance so newly spawned mobs do not despawn immediately"
        );
        ConfigValidation.require(server.mobMaxHp > 0.0 && Double.isFinite(server.mobMaxHp), "territory.server.mobMaxHp", "must be > 0 and finite");
        ConfigValidation.range(server.mobBuffThreshold, 0.0f, 1.0f, "territory.server.mobBuffThreshold");
        ConfigValidation.notNull(server.titleLow, "territory.server.titleLow");
        ConfigValidation.notNull(server.titleMid, "territory.server.titleMid");
        ConfigValidation.notNull(server.titleHigh, "territory.server.titleHigh");
        ConfigValidation.min(server.titleMidThreshold, 0, "territory.server.titleMidThreshold");
        ConfigValidation.require(server.titleHighThreshold >= server.titleMidThreshold, "territory.server.titleHighThreshold", "must be >= titleMidThreshold");
        ConfigValidation.range(server.erodedMobSunProofChance, 0.0f, 1.0f, "territory.server.erodedMobSunProofChance");
        ConfigValidation.min(server.erodedMobTempProtectionMinTicks, 0, "territory.server.erodedMobTempProtectionMinTicks");
        ConfigValidation.min(server.erodedMobTempProtectionRandomTicks, 0, "territory.server.erodedMobTempProtectionRandomTicks");
        ConfigValidation.min(server.erodedSkeletonBurnSeconds, 0.0f, "territory.server.erodedSkeletonBurnSeconds");
        ConfigValidation.range(server.spawnProtectionRadius, 0, 256, "territory.server.spawnProtectionRadius");
        ConfigValidation.range(server.playerClaimRadius, 0, 256, "territory.server.playerClaimRadius");
        ConfigValidation.min(server.playerClaimActivationDelaySeconds, 0, "territory.server.playerClaimActivationDelaySeconds");
        ConfigValidation.range(server.maxClaimsPerPlayer, 0, 64, "territory.server.maxClaimsPerPlayer");
    }

    public static class Server {

        public boolean enabled = true;

        public float miningWeight = 0.40f;

        public float pollutionWeight = 0.50f;

        public float forestWeight = 0.40f;

        public boolean caveCollapseEnabled = true;

        public int miningThreshold = 250;

        public long collapseCooldownMs = 3000;

        public int stabilizerRadius = 8;

        public int collapseMaxY = 50;

        public float collapseChanceLow = 0.08f;

        public float collapseChanceMid = 0.18f;

        public float collapseChanceHigh = 0.35f;

        public float collapseMobSpawnChance = 0.25f;

        public boolean ecosystemEnabled = true;

        public int ecosystemIntervalTicks = 100;

        public int ecosystemVisibleRadiusBlocks = 32;

        public int ecosystemMaxPlayersPerSlice = 10;

        public int ecosystemAttemptsPerPlayer = 6;

        public int ecosystemSurfaceAttempts = 4;

        public int ecosystemLeafAttempts = 2;

        public int ecosystemLeafMinY = 60;

        public int ecosystemLeafMaxY = 140;

        public int ecosystemCalmDownDelay = 1200;

        public boolean statePruningEnabled = true;

        /** How often inactive territory cells are checked (6000 ticks = 5 minutes). */
        public int statePruneIntervalTicks = 6000;

        /**
         * Forget a territory cell after this many ticks without real player
         * territory activity (432000 ticks = 6 hours of server world time).
         */
        public long stateRetentionTicks = 432000L;

        public float ecosystemDegradeThreatThreshold = 0.60f;

        public float ecosystemRegenThreatThreshold = 0.30f;

        public float grassDegradeChance = 0.20f;

        public float grassRegrowChance = 0.4f;

        public float permanentScarChance = 0.05f;

        public float ecosystemLeafLossMultiplier = 0.10f;

        public float ecosystemLeafLossMaxChance = 0.30f;

        public float ecosystemLeafLossMinChance = 0.02f;

        public boolean mobSpawnControlEnabled = true;

        public boolean surfaceOnlySpawns = true;

        public int undergroundTolerance = 5;

        public int spawnCheckInterval = 1200;

        public int spawnAttempts = 20;

        public double spawnMinDistance = 14.0;

        public double spawnMaxDistance = 40.0;

        public float spawnKeepMinChance = 0.08f;

        public int mobMaxPerChunk = 3;

        public int maxMobsPerSpawnCycle = 1;

        public int mobDespawnRadius = 64;

        public double mobMaxHp = 40.0;

        public boolean MobNameVisible = false;

        public boolean mobBuffEnabled = true;

        public float mobBuffThreshold = 0.40f;

        public String titleLow = "Forsaken";

        public String titleMid = "Eroded";

        public String titleHigh = "Apocalypse";

        public int titleMidThreshold = 250;

        public int titleHighThreshold = 1000;

        public float erodedMobSunProofChance = 0.35F;

        public int erodedMobTempProtectionMinTicks = 600;

        public int erodedMobTempProtectionRandomTicks = 1200;

        public float erodedSkeletonBurnSeconds = 8.0F;

        public boolean spawnProtectionEnabled = true;

        public int spawnProtectionRadius = 64;

        public boolean bypassCreative = true;

        public boolean bypassOP = true;

        public boolean preventExplosions = true;

        public boolean preventBlockBreak = true;

        public boolean preventBlockPlace = true;

        public boolean preventContainerUse = false;

        public boolean preventRedstoneControls = false;

        public boolean preventPressurePlates = false;

        public boolean preventProtectedEntityInteraction = true;

        public boolean preventPistonPush = true;

        public boolean preventFluidFlow = true;

        public boolean preventInventoryAutomationTransfer = true;

        public boolean preventDispenserDropperBoundaryActions = true;

        public boolean spawnPreventProjectileBoundaryActions = true;

        public boolean spawnPreventMobGriefing = true;

        public boolean spawnPreventVehicles = true;

        public boolean spawnPreventSpecialBlockUse = false;

        public boolean playerClaimProtectionEnabled = true;

        public int playerClaimRadius = 10;

        public int playerClaimActivationDelaySeconds = 60;

        public int maxClaimsPerPlayer = 4;

        public boolean preventPlayerClaimOverlap = true;

        public boolean claimCreativeBypass = true;

        public boolean protectClaimBlockBreak = true;

        public boolean protectClaimBlockPlace = true;

        public boolean protectClaimContainers = true;

        public boolean protectClaimFire = true;

        public boolean protectClaimExplosions = true;

        public boolean preventProjectileBoundaryActions = true;

        public boolean protectClaimMobGriefing = true;

        public boolean protectClaimVehicles = true;

        public boolean protectClaimFluidFlow = true;

        public boolean protectClaimInventoryAutomationTransfer = true;

        public boolean protectClaimDispenserDropperBoundaryActions = true;
    }
}

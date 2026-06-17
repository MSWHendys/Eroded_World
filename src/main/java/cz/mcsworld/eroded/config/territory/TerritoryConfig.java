package cz.mcsworld.eroded.config.territory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "ErodedWorld/territory")
public class TerritoryConfig implements ConfigData {

    @ConfigEntry.Category("server")
    public Server server = new Server();

    public static TerritoryConfig get() {
        return AutoConfig.getConfigHolder(TerritoryConfig.class).get();
    }

    public static class Server {

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public float miningWeight = 0.40f;

        @ConfigEntry.Gui.Tooltip
        public float pollutionWeight = 0.50f;

        @ConfigEntry.Gui.Tooltip
        public float forestWeight = 0.40f;

        @ConfigEntry.Gui.Tooltip
        public boolean caveCollapseEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public int miningThreshold = 250;

        @ConfigEntry.Gui.Tooltip
        public long collapseCooldownMs = 3000;

        @ConfigEntry.Gui.Tooltip
        public int stabilizerRadius = 8;

        @ConfigEntry.Gui.Tooltip
        public int collapseMaxY = 50;

        @ConfigEntry.Gui.Tooltip
        public float collapseChanceLow = 0.08f;

        @ConfigEntry.Gui.Tooltip
        public float collapseChanceMid = 0.18f;

        @ConfigEntry.Gui.Tooltip
        public float collapseChanceHigh = 0.35f;

        @ConfigEntry.Gui.Tooltip
        public float collapseMobSpawnChance = 0.25f;

        @ConfigEntry.Gui.Tooltip
        public boolean ecosystemEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemIntervalTicks = 100;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemVisibleRadiusBlocks = 32;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemMaxPlayersPerSlice = 10;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemAttemptsPerPlayer = 6;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemSurfaceAttempts = 4;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemLeafAttempts = 2;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemLeafMinY = 60;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemLeafMaxY = 140;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemCalmDownDelay = 1200;

        @ConfigEntry.Gui.Tooltip
        public float ecosystemDegradeThreatThreshold = 0.60f;

        @ConfigEntry.Gui.Tooltip
        public float ecosystemRegenThreatThreshold = 0.30f;

        @ConfigEntry.Gui.Tooltip
        public float grassDegradeChance = 0.20f;

        @ConfigEntry.Gui.Tooltip
        public float grassRegrowChance = 0.4f;

        @ConfigEntry.Gui.Tooltip
        public float permanentScarChance = 0.05f;

        @ConfigEntry.Gui.Tooltip
        public float ecosystemLeafLossMultiplier = 0.10f;

        @ConfigEntry.Gui.Tooltip
        public float ecosystemLeafLossMaxChance = 0.30f;

        @ConfigEntry.Gui.Tooltip
        public float ecosystemLeafLossMinChance = 0.02f;

        @ConfigEntry.Gui.Tooltip
        public boolean mobSpawnControlEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean surfaceOnlySpawns = true;

        @ConfigEntry.Gui.Tooltip
        public int undergroundTolerance = 5;

        @ConfigEntry.Gui.Tooltip
        public int spawnCheckInterval = 1200;

        @ConfigEntry.Gui.Tooltip
        public int spawnAttempts = 20;

        @ConfigEntry.Gui.Tooltip
        public double spawnMinDistance = 14.0;

        @ConfigEntry.Gui.Tooltip
        public double spawnMaxDistance = 40.0;

        @ConfigEntry.Gui.Tooltip
        public float spawnKeepMinChance = 0.08f;

        @ConfigEntry.Gui.Tooltip
        public int mobMaxPerChunk = 3;

        @ConfigEntry.Gui.Tooltip
        public int maxMobsPerSpawnCycle = 1;

        @ConfigEntry.Gui.Tooltip
        public int mobDespawnRadius = 64;

        @ConfigEntry.Gui.Tooltip
        public double mobMaxHp = 40.0;

        @ConfigEntry.Gui.Tooltip
        public boolean MobNameVisible = false;

        @ConfigEntry.Gui.Tooltip
        public boolean mobBuffEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public float mobBuffThreshold = 0.40f;

        @ConfigEntry.Gui.Tooltip
        public String titleLow = "Forsaken";

        @ConfigEntry.Gui.Tooltip
        public String titleMid = "Eroded";

        @ConfigEntry.Gui.Tooltip
        public String titleHigh = "Apocalypse";

        @ConfigEntry.Gui.Tooltip
        public int titleMidThreshold = 250;

        @ConfigEntry.Gui.Tooltip
        public int titleHighThreshold = 1000;

        @ConfigEntry.Gui.Tooltip
        public float erodedMobSunProofChance = 0.35F;

        @ConfigEntry.Gui.Tooltip
        public int erodedMobTempProtectionMinTicks = 600;

        @ConfigEntry.Gui.Tooltip
        public int erodedMobTempProtectionRandomTicks = 1200;

        @ConfigEntry.Gui.Tooltip
        public float erodedSkeletonBurnSeconds = 8.0F;

        @ConfigEntry.Gui.Tooltip
        public boolean spawnProtectionEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public int spawnProtectionRadius = 64;

        @ConfigEntry.Gui.Tooltip
        public boolean bypassCreative = true;

        @ConfigEntry.Gui.Tooltip
        public boolean bypassOP = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventExplosions = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventBlockBreak = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventBlockPlace = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventContainerUse = false;

        @ConfigEntry.Gui.Tooltip
        public boolean preventRedstoneControls = false;

        @ConfigEntry.Gui.Tooltip
        public boolean preventPressurePlates = false;

        @ConfigEntry.Gui.Tooltip
        public boolean preventProtectedEntityInteraction = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventPistonPush = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventFluidFlow = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventInventoryAutomationTransfer = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventDispenserDropperBoundaryActions = true;

        @ConfigEntry.Gui.Tooltip
        public boolean spawnPreventProjectileBoundaryActions = true;

        @ConfigEntry.Gui.Tooltip
        public boolean spawnPreventMobGriefing = true;

        @ConfigEntry.Gui.Tooltip
        public boolean spawnPreventVehicles = true;

        @ConfigEntry.Gui.Tooltip
        public boolean spawnPreventSpecialBlockUse = false;

        @ConfigEntry.Gui.Tooltip
        public boolean playerClaimProtectionEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public int playerClaimRadius = 10;

        @ConfigEntry.Gui.Tooltip
        public int playerClaimActivationDelaySeconds = 60;

        @ConfigEntry.Gui.Tooltip
        public int maxClaimsPerPlayer = 4;

        @ConfigEntry.Gui.Tooltip
        public boolean preventPlayerClaimOverlap = true;

        @ConfigEntry.Gui.Tooltip
        public boolean claimCreativeBypass = true;

        @ConfigEntry.Gui.Tooltip
        public boolean protectClaimBlockBreak = true;

        @ConfigEntry.Gui.Tooltip
        public boolean protectClaimBlockPlace = true;

        @ConfigEntry.Gui.Tooltip
        public boolean protectClaimContainers = true;

        @ConfigEntry.Gui.Tooltip
        public boolean protectClaimFire = true;

        @ConfigEntry.Gui.Tooltip
        public boolean protectClaimExplosions = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventProjectileBoundaryActions = true;

        @ConfigEntry.Gui.Tooltip
        public boolean protectClaimMobGriefing = true;

        @ConfigEntry.Gui.Tooltip
        public boolean protectClaimVehicles = true;

        @ConfigEntry.Gui.Tooltip
        public boolean protectClaimFluidFlow = true;

        @ConfigEntry.Gui.Tooltip
        public boolean protectClaimInventoryAutomationTransfer = true;

        @ConfigEntry.Gui.Tooltip
        public boolean protectClaimDispenserDropperBoundaryActions = true;
    }
}
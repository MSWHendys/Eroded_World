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

        /* ===================== VÝPOČET HROZBY ================= */
        @ConfigEntry.Gui.Tooltip
        public float miningWeight = 0.40f;

        @ConfigEntry.Gui.Tooltip
        public float pollutionWeight = 0.50f;

        @ConfigEntry.Gui.Tooltip
        public float forestWeight = 0.40f;

        /* ==================== JESKYNÍ SYSTÉM ================== */
        @ConfigEntry.Gui.Tooltip
        public boolean caveCollapseEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public int miningThreshold = 150;

        @ConfigEntry.Gui.Tooltip
        public long collapseCooldownMs = 3000;

        @ConfigEntry.Gui.Tooltip
        public int stabilizerRadius = 4;

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

        /* ===================== EKOSYSTÉM ====================== */
        @ConfigEntry.Gui.Tooltip
        public boolean ecosystemEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemIntervalTicks = 100;

        @ConfigEntry.Gui.Tooltip
        public float grassDegradeChance = 0.20f;

        @ConfigEntry.Gui.Tooltip
        public float grassRegrowChance = 0.4f;

        @ConfigEntry.Gui.Tooltip
        public float permanentScarChance = 0.05f;

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
        public float ecosystemDegradeThreatThreshold = 0.60f;

        @ConfigEntry.Gui.Tooltip
        public float ecosystemRegenThreatThreshold = 0.30f;

        @ConfigEntry.Gui.Tooltip
        public float ecosystemLeafLossMultiplier = 0.10f;

        @ConfigEntry.Gui.Tooltip
        public float ecosystemLeafLossMaxChance = 0.30f;

        @ConfigEntry.Gui.Tooltip
        public float ecosystemLeafLossMinChance = 0.02f;

        @ConfigEntry.Gui.Tooltip
        public int ecosystemCalmDownDelay = 1200;

        /* =================== SPAWN SYSTÉM ==================== */
        @ConfigEntry.Gui.Tooltip
        public boolean mobSpawnControlEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean surfaceOnlySpawns = true;

        @ConfigEntry.Gui.Tooltip
        public int undergroundTolerance = 5;

        @ConfigEntry.Gui.Tooltip
        public float spawnKeepMinChance = 0.15f;

        @ConfigEntry.Gui.Tooltip
        public double mobMaxHp = 40.0;

        @ConfigEntry.Gui.Tooltip
        public int mobMaxPerChunk = 3;

        @ConfigEntry.Gui.Tooltip
        public int spawnCheckInterval = 600;

        @ConfigEntry.Gui.Tooltip
        public int mobDespawnRadius = 64;

        @ConfigEntry.Gui.Tooltip
        public double spawnMinDistance = 16.0;

        @ConfigEntry.Gui.Tooltip
        public double spawnMaxDistance = 32.0;

        @ConfigEntry.Gui.Tooltip
        public int spawnAttempts = 20;

        @ConfigEntry.Gui.Tooltip
        public boolean MobNameVisible = false;

        /* =================== POSILOVÁNÍ BYTOSTÍ ============== */
        @ConfigEntry.Gui.Tooltip
        public boolean mobBuffEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public float mobBuffThreshold = 0.50f;


        /* =============== Názvy a hranice titulů =============== */
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

        // --- Počty mobů při spawnu ---
        @ConfigEntry.Gui.Tooltip
        public int maxMobsPerSpawnCycle = 2;
        // Maximální počet mobů, který se pokusí spawnout u jednoho hráče v jednom intervalu

        /* =================== SPAWN PROTECTION ==================== */
        @ConfigEntry.Gui.Tooltip
        public boolean spawnProtectionEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public int spawnProtectionRadius = 64;

        @ConfigEntry.Gui.Tooltip
        public boolean preventExplosions = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventBlockBreak = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventBlockPlace = true;

        @ConfigEntry.Gui.Tooltip
        public boolean preventPistonPush = true;

        @ConfigEntry.Gui.Tooltip
        public boolean bypassCreative = true;

        @ConfigEntry.Gui.Tooltip
        public boolean bypassOP = true;
        }


}
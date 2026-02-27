package cz.mcsworld.eroded.config.territory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "ErodedWorld/territory")
public class TerritoryConfig implements ConfigData {

    public static TerritoryConfig get() {
        return AutoConfig.getConfigHolder(TerritoryConfig.class).getConfig();
    }

    /* ========================================================= */
    /* ======================== SERVER ========================== */
    /* ========================================================= */

    @ConfigEntry.Category("server")
    public Server server = new Server();

    public static class Server {

        /* ===================================================== */
        /* ======================= GLOBÁLNÍ ===================== */
        /* ===================================================== */

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;
        // Hlavní zapnutí nebo vypnutí celého systému území

        /* ===================================================== */
        /* ===================== VÝPOČET HROZBY ================= */
        /* ===================================================== */

        @ConfigEntry.Gui.Tooltip
        public float miningWeight = 0.45f; //0.45f
        // Váha těžby při výpočtu úrovně ohrožení oblasti

        @ConfigEntry.Gui.Tooltip
        public float pollutionWeight = 0.45f; //0.45f
        // Váha znečištění při výpočtu úrovně ohrožení oblasti

        @ConfigEntry.Gui.Tooltip
        public float forestWeight = 0.30f; //0.30f
        // Váha zalesnění, která snižuje úroveň ohrožení oblasti

        /* ===================================================== */
        /* ==================== JESKYNÍ SYSTÉM ================== */
        /* ===================================================== */

        @ConfigEntry.Gui.Tooltip
        public boolean caveCollapseEnabled = true;
        // Zapnutí nebo vypnutí závalů v podzemí

        @ConfigEntry.Gui.Tooltip
        public int miningThreshold = 30;
        // Minimální hodnota těžby nutná pro možnost vzniku závalu

        @ConfigEntry.Gui.Tooltip
        public long collapseCooldownMs = 0;
        // Časová prodleva mezi dvěma závaly v jedné oblasti (v milisekundách)

        @ConfigEntry.Gui.Tooltip
        public int stabilizerRadius = 3;
        // Poloměr kontroly podpůrných bloků, které mohou závalu zabránit

        @ConfigEntry.Gui.Tooltip
        public int collapseMaxY = 50;
        // Nejvyšší výška, ve které může dojít k závalu

        @ConfigEntry.Gui.Tooltip
        public float collapseChanceLow = 0.02f;
        // Pravděpodobnost závalu při nízké míře těžby

        @ConfigEntry.Gui.Tooltip
        public float collapseChanceMid = 0.05f;
        // Pravděpodobnost závalu při střední míře těžby

        @ConfigEntry.Gui.Tooltip
        public float collapseChanceHigh = 0.1f;
        // Pravděpodobnost závalu při vysoké míře těžby

        @ConfigEntry.Gui.Tooltip
        public float collapseMobSpawnChance = 0.25f;
        // Pravděpodobnost výskytu nepřátelské bytosti při závalu

        @ConfigEntry.Gui.Tooltip
        public int collapseMobSpawnAttempts = 5;
        // Počet pokusů o nalezení vhodného místa pro výskyt bytosti

        /* ====================================================== */
        /* ===================== EKOSYSTÉM ====================== */
        /* ====================================================== */

        @ConfigEntry.Gui.Tooltip
        public boolean ecosystemEnabled = true;
        // Zapnutí nebo vypnutí změn krajiny v důsledku narušení oblasti

        @ConfigEntry.Gui.Tooltip
        public int ecosystemIntervalTicks = 100;
        // Interval vyhodnocování změn krajiny (v herních tazích)


        // ----- Povrchová degradace -----

        @ConfigEntry.Gui.Tooltip
        public float grassDegradeChance = 0.35f; //0.25f
        // Pravděpodobnost poškození travnatého povrchu

        @ConfigEntry.Gui.Tooltip
        public float grassRegrowChance = 0.5f;
        // Pravděpodobnost obnovy travnatého povrchu při nízkém ohrožení

        @ConfigEntry.Gui.Tooltip
        public float permanentScarChance = 0.1f;//0.1f
        // Pravděpodobnost trvalého poškození krajiny

    /**    @ConfigEntry.Gui.Tooltip
        public float erosionChance = 0.25f; //0.25f
        // Pravděpodobnost přeměny kamene na štěrk

        @ConfigEntry.Gui.Tooltip
        public int pollutionRegenThreshold = 50;
        // Maximální míra znečištění, při které je ještě možná obnova krajiny

        @ConfigEntry.Gui.Tooltip
        public int pollutionScarThreshold = 80;
        // Minimální míra znečištění potřebná pro vznik trvalého poškození
    */

        @ConfigEntry.Gui.Tooltip
        public int ecosystemVisibleRadiusBlocks = 28;
        // Poloměr viditelných změn krajiny kolem hráče (v blocích)

        @ConfigEntry.Gui.Tooltip
        public int ecosystemMaxPlayersPerSlice = 10;
        // Kolik hráčů se maximálně zpracuje v jednom vyhodnocení (ochrana výkonu serveru)

        @ConfigEntry.Gui.Tooltip
        public int ecosystemAttemptsPerPlayer = 8;
        // Kolik náhodných pokusů o změnu krajiny se udělá na jednoho hráče (vyšší = rychlejší změny, ale náročnější)

        @ConfigEntry.Gui.Tooltip
        public int ecosystemSurfaceAttempts = 5;
        // Kolik pokusů z celkového počtu jde na povrch (tráva/hlína)

        @ConfigEntry.Gui.Tooltip
        public int ecosystemLeafAttempts = 3;
        // Kolik pokusů z celkového počtu jde na listí stromů (řídnutí korun)

        @ConfigEntry.Gui.Tooltip
        public int ecosystemLeafMinY = 55;
        // Nejnižší výška, ve které se bude hledat listí stromů (aby se netrefovala zem)

        @ConfigEntry.Gui.Tooltip
        public int ecosystemLeafMaxY = 140;
        // Nejvyšší výška, ve které se bude hledat listí stromů

        @ConfigEntry.Gui.Tooltip
        public float ecosystemDegradeThreatThreshold = 0.55f;
        // Od jaké úrovně ohrožení se spustí viditelná degradace (povrch a listí)

        @ConfigEntry.Gui.Tooltip
        public float ecosystemRegenThreatThreshold = 0.25f;
        // Pod jakou úrovní ohrožení se spustí uzdravování krajiny (pokud je i nízké znečištění)

        @ConfigEntry.Gui.Tooltip
        public float ecosystemLeafLossMultiplier = 0.10f;
        // Násobitel pro ztrátu listí (použije se spolu s pravděpodobností trvalého poškození)

        @ConfigEntry.Gui.Tooltip
        public float ecosystemLeafLossMaxChance = 0.35f;
        // Horní limit pravděpodobnosti, aby systém nikdy nebyl příliš agresivní

        @ConfigEntry.Gui.Tooltip
        public float ecosystemLeafLossMinChance = 0.03f;
        // Dolní limit pravděpodobnosti, aby systém nebyl úplně neviditelný

        @ConfigEntry.Gui.Tooltip
        public int ecosystemCalmDownDelay = 1000;
        // Čas potřebný pro regeneraci

        /* ===================================================== */
        /* =================== SPAWN SYSTÉM ==================== */
        /* ===================================================== */

        @ConfigEntry.Gui.Tooltip
        public boolean mobSpawnControlEnabled = true;
        // Zapnutí nebo vypnutí řízení spawn systému (povrch + redukce počtu)

        @ConfigEntry.Gui.Tooltip
        public boolean surfaceOnlySpawns = true;
        // Pokud true, mobové pod povrchem budou odstraněni

        @ConfigEntry.Gui.Tooltip
        public int undergroundTolerance = 6;
        // Kolik bloků pod povrchem je ještě povoleno

        @ConfigEntry.Gui.Tooltip
        public float spawnKeepMinChance = 0.4f;
        // Minimální procento spawnů při maximálním threat (0.4 = 40%)

        @ConfigEntry.Gui.Tooltip
        public double mobMaxHp = 40.0;
        // Maximální HP při threat = 1.0

        /* ===================================================== */
        /* =================== POSILOVÁNÍ BYTOSTÍ ============== */
        /* ===================================================== */

        @ConfigEntry.Gui.Tooltip
        public boolean mobBuffEnabled = true;
        // Zapnutí nebo vypnutí zesílení nepřátelských bytostí

        @ConfigEntry.Gui.Tooltip
        public float mobBuffThreshold = 0.1f; //0.35f
        // Minimální úroveň ohrožení potřebná pro zesílení bytostí

        /* ===================================================== */
        /* ================= OCHRANA MÍSTA VZNIKU =============== */
        /* ===================================================== */

        @ConfigEntry.Gui.Tooltip
        public boolean spawnProtectionEnabled = true;
        // Zapnutí nebo vypnutí ochrany oblasti prvotního výskytu hráčů

        @ConfigEntry.Gui.Tooltip
        public int spawnProtectionRadius = 40;
        // Poloměr chráněné oblasti kolem místa prvotního výskytu

        /* ===================================================== */
        /* ======================== LADĚNÍ ====================== */
        /* ===================================================== */

        @ConfigEntry.Gui.Tooltip
        public boolean debugMessages = true;
        // Zobrazení ladicích zpráv v konzoli serveru
    }
}
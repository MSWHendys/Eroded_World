package cz.mcsworld.eroded.config.energy;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;


@Config(name = "ErodedWorld/energy")
public class EnergyConfig implements ConfigData {

    public static EnergyConfig get() {
        return AutoConfig
                .getConfigHolder(EnergyConfig.class)
                .getConfig();
    }

    /* ======================================================
       SERVER ČÁST – OVLIVŇUJE GAMEPLAY
       ====================================================== */

    @ConfigEntry.Gui.CollapsibleObject
    public Server server = new Server();

    public static class Server {

        /** Globální zapnutí / vypnutí ENERGY systému */
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        /* ---------- CORE ---------- */

        @ConfigEntry.Gui.CollapsibleObject
        public Core core = new Core();

        public static class Core {

            @ConfigEntry.Gui.Tooltip // Maximální energie hráče
            public int maxEnergy = 100;

            @ConfigEntry.Gui.Tooltip // Kolik energie má jeden segment
            public int energyPerSegment = 1;

            @ConfigEntry.Gui.Tooltip // Zakáže práci pokud je energie 0
            public boolean blockWorkAtZero = true;
        }

        /* ---------- REGENERACE ---------- */

        @ConfigEntry.Gui.CollapsibleObject
        public Regen regen = new Regen();

        public static class Regen {

            @ConfigEntry.Gui.Tooltip //Zapnout pasivní regeneraci
            public boolean passiveRegenEnabled = true;

            @ConfigEntry.Gui.Tooltip //Interval regenerace (sekundy)
            public int regenIntervalSeconds = 15;
        }

        /* ---------- SPÁNEK ---------- */

        @ConfigEntry.Gui.CollapsibleObject
        public Sleep sleep = new Sleep();

        public static class Sleep {

            @ConfigEntry.Gui.Tooltip // Spánek obnoví plnou energii
            public boolean sleepRestoresFull = true;
        }

        /* ---------- JÍDLO ---------- */

        @ConfigEntry.Gui.CollapsibleObject
        public Food food = new Food();

        public static class Food {

            @ConfigEntry.Gui.Tooltip // Obnovení energie – malá svačina
            public int foodRestoreSnack = 2;

            @ConfigEntry.Gui.Tooltip // Jednoduché jídlo
            public int foodRestoreSimple = 5;

            @ConfigEntry.Gui.Tooltip // Plnohodnotné jídlo
            public int foodRestoreMeal = 10;

            @ConfigEntry.Gui.Tooltip // Hostina
            public int foodRestoreFeast = 18;
        }

        /* ---------- KOLAPS ---------- */

        @ConfigEntry.Gui.CollapsibleObject
        public Collapse collapse = new Collapse();

        public static class Collapse {

            @ConfigEntry.Gui.Tooltip //Délka před regenerací energie hráče v ms
            public int collapseDelayMs = 5000;
        }

        /* ---------- VAROVÁNÍ ---------- */

        @ConfigEntry.Gui.CollapsibleObject
        public Warnings warnings = new Warnings();

        public static class Warnings {

            @ConfigEntry.Gui.Tooltip // Zapnout varování
            public boolean warningsEnabled = true;

            @ConfigEntry.Gui.Tooltip // Cooldown mezi varováními (ms)
            public int warningCooldownMs = 1500;
        }

        @ConfigEntry.Gui.CollapsibleObject
        public Thresholds thresholds = new Thresholds();

        public static class Thresholds {

            @ConfigEntry.Gui.Tooltip // % a méně = EMPTY (default 1 %)
            public float emptyPercent = 1f;

            @ConfigEntry.Gui.Tooltip // % a méně = EXHAUSTED (default 25 %)
            public float exhaustedPercent = 25f;

            @ConfigEntry.Gui.Tooltip // % a méně = TIRED (default 51 %)
            public float tiredPercent = 51f;
        }

        @ConfigEntry.Gui.CollapsibleObject
        public HudThresholds hudThresholds = new HudThresholds();

        public static class HudThresholds {

            @ConfigEntry.Gui.Tooltip // Od kolika % je barva GREEN (default 51 %)
            public float greenFromPercent = 51f;

            @ConfigEntry.Gui.Tooltip // Od kolika % je barva YELLOW (default 35 %)
            public float yellowFromPercent = 35f;

            @ConfigEntry.Gui.Tooltip // Od kolika % je barva ORANGE (default 25 %)
            public float orangeFromPercent = 25f;

            @ConfigEntry.Gui.Tooltip // Pod kolika % začne blinkovat “leading icon” (default 20 %)
            public float blinkBelowPercent = 20f;
        }
    }

    /* ======================================================
       CLIENT ČÁST – POUZE VIZUÁL
       ====================================================== */

    @ConfigEntry.Gui.CollapsibleObject
    public Client client = new Client();

    public static class Client {

        @ConfigEntry.Gui.CollapsibleObject
        public Hud hud = new Hud();

        public static class Hud {

            @ConfigEntry.Gui.Tooltip // Zapnout HUD
            public boolean energyHudEnabled = true;

            @ConfigEntry.Gui.Tooltip // Zobrazovat i při plné energii
            public boolean showHudWhenFull = false;

            @ConfigEntry.Gui.Tooltip // Počet segmentů
            public int numberEnergyFlashes = 10;

            @ConfigEntry.Gui.Tooltip // Pozice HUD
            public EnergyHudPosition hudPosition = EnergyHudPosition.LEFT_DOWN;

            @ConfigEntry.Gui.Tooltip
            public int hudMargin = 6;

            @ConfigEntry.Gui.Tooltip
            public int posIconHUD_Y = 15;

            @ConfigEntry.Gui.Tooltip
            public int posTextHUD_Y = 10;

            @ConfigEntry.Gui.Tooltip // Doba zobrazení varování
            public int warningMessageTime = 60;

        }
    }
}
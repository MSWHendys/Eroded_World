package cz.mcsworld.eroded.config.energy;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;


@Config(name = "ErodedWorld/energy")
public class EnergyConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public Server server = new Server();

    @ConfigEntry.Gui.CollapsibleObject
    public Client client = new Client();

    public static EnergyConfig get() {
        return AutoConfig
                .getConfigHolder(EnergyConfig.class)
                .getConfig();
    }


    public static class Server {

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;


        @ConfigEntry.Gui.CollapsibleObject
        public Core core = new Core();
        @ConfigEntry.Gui.CollapsibleObject
        public Regen regen = new Regen();

        @ConfigEntry.Gui.CollapsibleObject
        public Sleep sleep = new Sleep();
        @ConfigEntry.Gui.CollapsibleObject
        public Food food = new Food();

        @ConfigEntry.Gui.CollapsibleObject
        public Collapse collapse = new Collapse();
        @ConfigEntry.Gui.CollapsibleObject
        public Warnings warnings = new Warnings();

        @ConfigEntry.Gui.CollapsibleObject
        public Thresholds thresholds = new Thresholds();
        @ConfigEntry.Gui.CollapsibleObject
        public HudThresholds hudThresholds = new HudThresholds();

        @ConfigEntry.Gui.Tooltip
        public boolean fatigueWhenExhausted = true;

        public static class Core {

            @ConfigEntry.Gui.Tooltip
            public int maxEnergy = 100;

            @ConfigEntry.Gui.Tooltip
            public int energyPerSegment = 1;

            @ConfigEntry.Gui.Tooltip
            public boolean blockWorkAtZero = true;

            @ConfigEntry.Gui.Tooltip
            public float miningCost = 0.001f;
        }


        public static class Regen {

            @ConfigEntry.Gui.Tooltip
            public boolean passiveRegenEnabled = true;

            @ConfigEntry.Gui.Tooltip
            public int regenIntervalSeconds = 15;
        }

        public static class Sleep {

            @ConfigEntry.Gui.Tooltip
            public boolean sleepRestoresFull = true;
        }

        public static class Food {


            @ConfigEntry.Gui.Tooltip
            public int fruitBase = 2;
            @ConfigEntry.Gui.Tooltip
            public int vegetableBase = 2;
            @ConfigEntry.Gui.Tooltip
            public int grainBase = 3;
            @ConfigEntry.Gui.Tooltip
            public int meatBase = 4;
            @ConfigEntry.Gui.Tooltip
            public int fishBase = 3;
            @ConfigEntry.Gui.Tooltip
            public int mealBase = 6;

            @ConfigEntry.Gui.Tooltip
            public float rawMultiplier = 0.6f;
            @ConfigEntry.Gui.Tooltip
            public float cookedMultiplier = 1.2f;
            @ConfigEntry.Gui.Tooltip
            public float processedMultiplier = 1.5f;

            @ConfigEntry.Gui.Tooltip
            public float specialMultiplier = 2.0f;

            @ConfigEntry.Gui.Tooltip
            public int dangerousEnergyPenalty = 4;

        }

        public static class Collapse {

            @ConfigEntry.Gui.Tooltip
            public int collapseDelayMs = 5000;
        }

        public static class Warnings {

            @ConfigEntry.Gui.Tooltip
            public boolean warningsEnabled = true;

            @ConfigEntry.Gui.Tooltip
            public int warningCooldownMs = 1500;
        }

        public static class Thresholds {

            @ConfigEntry.Gui.Tooltip
            public float emptyPercent = 1f;

            @ConfigEntry.Gui.Tooltip
            public float exhaustedPercent = 25f;

            @ConfigEntry.Gui.Tooltip
            public float tiredPercent = 51f;
        }

        public static class HudThresholds {

            @ConfigEntry.Gui.Tooltip
            public float greenFromPercent = 51f;

            @ConfigEntry.Gui.Tooltip
            public float yellowFromPercent = 35f;

            @ConfigEntry.Gui.Tooltip
            public float orangeFromPercent = 25f;

            @ConfigEntry.Gui.Tooltip
            public float blinkBelowPercent = 20f;


        }


    }

    public static class Client {

        @ConfigEntry.Gui.CollapsibleObject
        public Hud hud = new Hud();

        public static class Hud {

            @ConfigEntry.Gui.Tooltip
            public boolean energyHudEnabled = true;

            @ConfigEntry.Gui.Tooltip
            public boolean showHudWhenFull = false;

            @ConfigEntry.Gui.Tooltip
            public int numberEnergyFlashes = 10;

            @ConfigEntry.Gui.Tooltip
            public EnergyHudPosition hudPosition = EnergyHudPosition.LEFT_DOWN;

            @ConfigEntry.Gui.Tooltip
            public int hudMargin = 6;

            @ConfigEntry.Gui.Tooltip
            public int posIconHUD_Y = 15;

            @ConfigEntry.Gui.Tooltip
            public int posTextHUD_Y = 40;

            @ConfigEntry.Gui.Tooltip
            public int warningMessageTime = 60;

        }
    }
}
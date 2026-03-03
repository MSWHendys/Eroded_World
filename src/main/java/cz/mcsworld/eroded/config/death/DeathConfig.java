package cz.mcsworld.eroded.config.death;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "ErodedWorld/death")
public class DeathConfig implements ConfigData {

    public static DeathConfig get() {
        return AutoConfig
                .getConfigHolder(DeathConfig.class)
                .getConfig();
    }


    @ConfigEntry.Gui.CollapsibleObject
    public Compass compass = new Compass();

    public static class Compass {

        @ConfigEntry.Gui.CollapsibleObject
        public Heartbeat heartbeat = new Heartbeat();

        @ConfigEntry.Gui.CollapsibleObject
        public Stress stress = new Stress();

        @ConfigEntry.Gui.CollapsibleObject
        public Client client = new Client();

        public static class Heartbeat {

            @ConfigEntry.Gui.Tooltip
            public int minInterval = 40;

            @ConfigEntry.Gui.Tooltip
            public int maxInterval = 120;

            @ConfigEntry.Gui.Tooltip
            public double dropCleanupRadius = 2.5;
        }

        public static class Stress {

            @ConfigEntry.Gui.Tooltip
            public double minDistance = 8.0;

            @ConfigEntry.Gui.Tooltip
            public double maxDistance = 128.0;
        }

        public static class Client {

            @ConfigEntry.Gui.Tooltip
            public int heartbeatCooldown = 100;
        }
    }

    @ConfigEntry.Gui.CollapsibleObject
    public Chest chest = new Chest();

    public static class Chest {

        @ConfigEntry.Gui.Tooltip
        public int protectionTicks = 6000;
    }

    @ConfigEntry.Gui.CollapsibleObject
    public Hologram hologram = new Hologram();

    public static class Hologram {

        @ConfigEntry.Gui.Tooltip
        public float rotationSpeed = 3.0f;

        @ConfigEntry.Gui.Tooltip
        public float bobbingSpeed = 0.1f;

        @ConfigEntry.Gui.Tooltip
        public float bobbingAmplitude = 0.05f;
    }
    @ConfigEntry.Gui.CollapsibleObject
    public Protection protection = new Protection();

    public static class Protection {

        @ConfigEntry.Gui.Tooltip
        public boolean distanceBased = true;

        @ConfigEntry.Gui.Tooltip
        public int minMinutes = 3;

        @ConfigEntry.Gui.Tooltip
        public int maxMinutes = 20;

        @ConfigEntry.Gui.Tooltip
        public double minutesPerBlock = 0.01;

        @ConfigEntry.Gui.Tooltip
        public boolean useSpawnIfNoBed = true;
    }
}

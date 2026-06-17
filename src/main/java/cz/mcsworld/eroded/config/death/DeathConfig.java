package cz.mcsworld.eroded.config.death;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "ErodedWorld/death")
public class DeathConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public Chest chest = new Chest();

    @ConfigEntry.Gui.CollapsibleObject
    public Protection protection = new Protection();

    @ConfigEntry.Gui.CollapsibleObject
    public RespawnProtection respawnProtection = new RespawnProtection();

    @ConfigEntry.Gui.CollapsibleObject
    public Compass compass = new Compass();

    @ConfigEntry.Gui.CollapsibleObject
    public CompassWeapon compassWeapon = new CompassWeapon();

    @ConfigEntry.Gui.CollapsibleObject
    public Hologram hologram = new Hologram();

    public static DeathConfig get() {
        return AutoConfig
                .getConfigHolder(DeathConfig.class)
                .getConfig();
    }

    public static class Chest {

        @ConfigEntry.Gui.Tooltip
        public int protectionTicks = 6000;
    }

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

    public static class RespawnProtection {

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public int durationTicks = 600;

        @ConfigEntry.Gui.Tooltip
        public double clearTargetRadius = 12.0;

        @ConfigEntry.Gui.Tooltip
        public boolean preventDamage = true;

        @ConfigEntry.Gui.Tooltip
        public boolean clearMobTargets = true;

        @ConfigEntry.Gui.Tooltip
        public boolean cancelOnAttack = true;

        @ConfigEntry.Gui.Tooltip
        public boolean showMessage = true;
    }

    public static class Compass {

        @ConfigEntry.Gui.CollapsibleObject
        public Heartbeat heartbeat = new Heartbeat();

        @ConfigEntry.Gui.CollapsibleObject
        public Stress stress = new Stress();

        @ConfigEntry.Gui.CollapsibleObject
        public DarknessBreak darknessBreak = new DarknessBreak();

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

        public static class DarknessBreak {

            @ConfigEntry.Gui.Tooltip
            public boolean enabled = true;

            @ConfigEntry.Gui.Tooltip
            public int durationTicks = 160;

            @ConfigEntry.Gui.Tooltip
            public int cooldownTicks = 600;

            @ConfigEntry.Gui.Tooltip
            public float maxDarkness = 0.25F;

            @ConfigEntry.Gui.Tooltip
            public boolean requireValidTarget = true;
        }
    }

    public static class CompassWeapon {

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public float damage = 8.0F;

        @ConfigEntry.Gui.Tooltip
        public int cooldownTicks = 13;

        @ConfigEntry.Gui.Tooltip
        public boolean damageHostileMobs = true;

        @ConfigEntry.Gui.Tooltip
        public boolean damagePlayers = true;

        @ConfigEntry.Gui.Tooltip
        public boolean damageOtherLivingEntities = false;

        @ConfigEntry.Gui.Tooltip
        public boolean requireActiveDeathMemory = true;
    }

    public static class Hologram {

        @ConfigEntry.Gui.Tooltip
        public float rotationSpeed = 3.0f;

        @ConfigEntry.Gui.Tooltip
        public float bobbingSpeed = 0.1f;

        @ConfigEntry.Gui.Tooltip
        public float bobbingAmplitude = 0.05f;
    }
}
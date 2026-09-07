package cz.mcsworld.eroded.config.death;

import cz.mcsworld.eroded.config.ConfigValidation;
import cz.mcsworld.eroded.config.ConfigValidationException;
import cz.mcsworld.eroded.config.ErodedConfig;
import cz.mcsworld.eroded.config.ErodedConfigs;

public class DeathConfig implements ErodedConfig {

    public Chest chest = new Chest();

    public Protection protection = new Protection();

    public RespawnProtection respawnProtection = new RespawnProtection();

    public Compass compass = new Compass();

    public CompassWeapon compassWeapon = new CompassWeapon();

    public Hologram hologram = new Hologram();

    public static DeathConfig get() {
        return ErodedConfigs.DEATH;
    }

    @Override
    public void validatePostLoad() throws ConfigValidationException {
        ConfigValidation.notNull(chest, "death.chest");
        ConfigValidation.notNull(protection, "death.protection");
        ConfigValidation.notNull(respawnProtection, "death.respawnProtection");
        ConfigValidation.notNull(compass, "death.compass");
        ConfigValidation.notNull(compass.heartbeat, "death.compass.heartbeat");
        ConfigValidation.notNull(compass.stress, "death.compass.stress");
        ConfigValidation.notNull(compass.darknessBreak, "death.compass.darknessBreak");
        ConfigValidation.notNull(compassWeapon, "death.compassWeapon");
        ConfigValidation.notNull(hologram, "death.hologram");

        ConfigValidation.min(chest.protectionTicks, 0, "death.chest.protectionTicks");
        ConfigValidation.min(protection.minMinutes, 0, "death.protection.minMinutes");
        ConfigValidation.require(protection.maxMinutes >= protection.minMinutes, "death.protection.maxMinutes", "must be >= minMinutes");
        ConfigValidation.min(protection.minutesPerBlock, 0.0, "death.protection.minutesPerBlock");
        ConfigValidation.min(respawnProtection.durationTicks, 0, "death.respawnProtection.durationTicks");
        ConfigValidation.range(respawnProtection.clearTargetRadius, 0.0, 128.0, "death.respawnProtection.clearTargetRadius");
        ConfigValidation.min(compass.heartbeat.minInterval, 1, "death.compass.heartbeat.minInterval");
        ConfigValidation.require(compass.heartbeat.maxInterval >= compass.heartbeat.minInterval, "death.compass.heartbeat.maxInterval", "must be >= minInterval");
        ConfigValidation.range(compass.heartbeat.dropCleanupRadius, 0.0, 128.0, "death.compass.heartbeat.dropCleanupRadius");
        ConfigValidation.min(compass.stress.minDistance, 0.0, "death.compass.stress.minDistance");
        ConfigValidation.require(compass.stress.maxDistance >= compass.stress.minDistance && Double.isFinite(compass.stress.maxDistance), "death.compass.stress.maxDistance", "must be finite and >= minDistance");
        ConfigValidation.min(compass.darknessBreak.durationTicks, 0, "death.compass.darknessBreak.durationTicks");
        ConfigValidation.min(compass.darknessBreak.cooldownTicks, 0, "death.compass.darknessBreak.cooldownTicks");
        ConfigValidation.range(compass.darknessBreak.maxDarkness, 0.0f, 1.0f, "death.compass.darknessBreak.maxDarkness");
        ConfigValidation.min(compassWeapon.damage, 0.0f, "death.compassWeapon.damage");
        ConfigValidation.min(compassWeapon.cooldownTicks, 0, "death.compassWeapon.cooldownTicks");
        ConfigValidation.finite(hologram.rotationSpeed, "death.hologram.rotationSpeed");
        ConfigValidation.finite(hologram.bobbingSpeed, "death.hologram.bobbingSpeed");
        ConfigValidation.min(hologram.bobbingAmplitude, 0.0f, "death.hologram.bobbingAmplitude");
    }

    public static class Chest {

        public int protectionTicks = 6000;
    }

    public static class Protection {

        public boolean distanceBased = true;

        public int minMinutes = 3;

        public int maxMinutes = 20;

        public double minutesPerBlock = 0.01;

        public boolean useSpawnIfNoBed = true;
    }

    public static class RespawnProtection {

        public boolean enabled = true;

        public int durationTicks = 600;

        public double clearTargetRadius = 12.0;

        public boolean preventDamage = true;

        public boolean clearMobTargets = true;

        public boolean cancelOnAttack = true;

        public boolean showMessage = true;
    }

    public static class Compass {

        public Heartbeat heartbeat = new Heartbeat();

        public Stress stress = new Stress();

        public DarknessBreak darknessBreak = new DarknessBreak();

        public static class Heartbeat {

            public int minInterval = 40;

            public int maxInterval = 120;

            public double dropCleanupRadius = 2.5;
        }

        public static class Stress {

            public double minDistance = 8.0;

            public double maxDistance = 128.0;
        }

        public static class DarknessBreak {

            public boolean enabled = true;

            public int durationTicks = 160;

            public int cooldownTicks = 600;

            public float maxDarkness = 0.25F;

            public boolean requireValidTarget = true;
        }
    }

    public static class CompassWeapon {

        public boolean enabled = true;

        public float damage = 8.0F;

        public int cooldownTicks = 13;

        public boolean damageHostileMobs = true;

        public boolean damagePlayers = true;

        public boolean damageOtherLivingEntities = false;

        public boolean requireActiveDeathMemory = true;
    }

    public static class Hologram {

        public float rotationSpeed = 3.0f;

        public float bobbingSpeed = 0.1f;

        public float bobbingAmplitude = 0.05f;
    }
}

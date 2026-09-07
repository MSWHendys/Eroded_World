package cz.mcsworld.eroded.config.combat;

import cz.mcsworld.eroded.config.ConfigValidation;
import cz.mcsworld.eroded.config.ConfigValidationException;
import cz.mcsworld.eroded.config.ErodedConfig;
import cz.mcsworld.eroded.config.ErodedConfigs;

public class CombatConfig implements ErodedConfig {

    public boolean enabled = true;

    public boolean debug = false;

    public Sprint sprint = new Sprint();

    public Dodge dodge = new Dodge();

    public static CombatConfig get() {
        return ErodedConfigs.COMBAT;
    }

    @Override
    public void validatePostLoad() throws ConfigValidationException {
        ConfigValidation.notNull(sprint, "combat.sprint");
        ConfigValidation.notNull(dodge, "combat.dodge");

        ConfigValidation.min(sprint.drainIntervalTicks, 1, "combat.sprint.drainIntervalTicks");
        ConfigValidation.min(sprint.energyPerInterval, 0, "combat.sprint.energyPerInterval");
        ConfigValidation.min(sprint.minEnergyToSprint, 0, "combat.sprint.minEnergyToSprint");

        ConfigValidation.min(dodge.cooldownTicks, 0, "combat.dodge.cooldownTicks");
        ConfigValidation.min(dodge.energyCost, 0, "combat.dodge.energyCost");
        ConfigValidation.range(dodge.maxDistance, 0.0, 16.0, "combat.dodge.maxDistance");
        ConfigValidation.range(dodge.stepSize, 0.05, 4.0, "combat.dodge.stepSize");
    }

    public static class Sprint {

        public boolean enabled = true;

        public int drainIntervalTicks = 10;

        public int energyPerInterval = 1;

        public int minEnergyToSprint = 2;

        public boolean stopSprintWhenEmpty = true;
    }

    public static class Dodge {

        public boolean enabled = true;

        public int cooldownTicks = 60;

        public int energyCost = 15;

        public double maxDistance = 2.8;

        public double stepSize = 0.25;

        public boolean allowBackward = true;

        public boolean allowSideways = true;
    }
}

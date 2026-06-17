package cz.mcsworld.eroded.config.combat;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "ErodedWorld/combat")
public class CombatConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip
    public boolean debug = false;

    @ConfigEntry.Gui.CollapsibleObject
    public Sprint sprint = new Sprint();

    @ConfigEntry.Gui.CollapsibleObject
    public Dodge dodge = new Dodge();

    public static CombatConfig get() {
        return AutoConfig
                .getConfigHolder(CombatConfig.class)
                .getConfig();
    }

    public static class Sprint {

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public int drainIntervalTicks = 10;

        @ConfigEntry.Gui.Tooltip
        public int energyPerInterval = 1;

        @ConfigEntry.Gui.Tooltip
        public int minEnergyToSprint = 2;

        @ConfigEntry.Gui.Tooltip
        public boolean stopSprintWhenEmpty = true;
    }

    public static class Dodge {

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public int cooldownTicks = 60;

        @ConfigEntry.Gui.Tooltip
        public int energyCost = 15;

        @ConfigEntry.Gui.Tooltip
        public double maxDistance = 2.8;

        @ConfigEntry.Gui.Tooltip
        public double stepSize = 0.25;

        @ConfigEntry.Gui.Tooltip
        public boolean allowBackward = true;

        @ConfigEntry.Gui.Tooltip
        public boolean allowSideways = true;
    }
}
package cz.mcsworld.eroded.config.combat;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "ErodedWorld/combat")
public class CombatConfig implements ConfigData {
    public static CombatConfig get() {
        return AutoConfig
                .getConfigHolder(CombatConfig.class)
                .getConfig();
    }
    public int dodgeEnergyCost = 2;
    public int sprintEnergyCostPerSecond = 1;

    @ConfigEntry.Gui.CollapsibleObject
    public Dodge dodge = new Dodge();

    public static class Dodge {

        /**
         * Cooldown mezi úskoky (ticků)
         */
        @ConfigEntry.Gui.Tooltip
        public int cooldownTicks = 60;

        /**
         * Spotřeba energie za úskok
         */
        @ConfigEntry.Gui.Tooltip
        public int energyCost = 15;

        /**
         * Celková vzdálenost úskoku
         */
        @ConfigEntry.Gui.Tooltip
        public double distance = 2.8;

        /**
         * Krok pohybu (plynulost)
         */
        @ConfigEntry.Gui.Tooltip
        public double step = 0.25;
    }

    @ConfigEntry.Gui.CollapsibleObject
    public Sprint sprint = new Sprint();

    public static class Sprint {

        /**
         * Jak často se odebírá energie (v tickech)
         */
        @ConfigEntry.Gui.Tooltip
        public int drainIntervalTicks = 10;

        /**
         * Kolik energie se odebere za interval
         */
        @ConfigEntry.Gui.Tooltip
        public int energyPerInterval = 1;

        /**
         * Minimální energie nutná pro sprint
         */
        @ConfigEntry.Gui.Tooltip
        public int minEnergyToSprint = 2;
    }
}

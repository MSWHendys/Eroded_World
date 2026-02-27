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

    /* --------------------------------------------------------
       GLOBAL
     -------------------------------------------------------- */

    @ConfigEntry.Gui.Tooltip
    // Zapne / vypne celý combat systém (dodge + sprint energie)
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip
    // Zapne debug výpisy pro combat systém (vývoj / ladění)
    public boolean debug = false;

    /* --------------------------------------------------------
       DODGE
     -------------------------------------------------------- */

    @ConfigEntry.Gui.CollapsibleObject
    public Dodge dodge = new Dodge();

    public static class Dodge {

        @ConfigEntry.Gui.Tooltip
        // Povolit úhyb (dodge mechaniku)
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        // Cooldown mezi úhyby (v tickech, 20 ticků = 1 sekunda)
        public int cooldownTicks = 60;

        @ConfigEntry.Gui.Tooltip
        // Cena úhybu v energii
        public int energyCost = 15;

        @ConfigEntry.Gui.Tooltip
        // Maximální vzdálenost úhybu (v blocích)
        public double maxDistance = 2.8;

        @ConfigEntry.Gui.Tooltip
        // Krok kontroly kolize během úhybu (menší hodnota = přesnější, ale náročnější)
        public double stepSize = 0.25;

        @ConfigEntry.Gui.Tooltip
        // Povolit úhyb dozadu
        public boolean allowBackward = true;

        @ConfigEntry.Gui.Tooltip
        // Povolit úhyb do stran
        public boolean allowSideways = true;
    }

    /* --------------------------------------------------------
       SPRINT
     -------------------------------------------------------- */

    @ConfigEntry.Gui.CollapsibleObject
    public Sprint sprint = new Sprint();

    public static class Sprint {

        @ConfigEntry.Gui.Tooltip
        // Povolit spotřebu energie při sprintu
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        // Interval spotřeby energie při sprintu (v tickech)
        public int drainIntervalTicks = 10;

        @ConfigEntry.Gui.Tooltip
        // Kolik energie se odečte za jeden interval sprintu
        public int energyPerInterval = 1;

        @ConfigEntry.Gui.Tooltip
        // Minimální energie potřebná k zahájení / udržení sprintu
        public int minEnergyToSprint = 2;

        @ConfigEntry.Gui.Tooltip
        // Automaticky zastavit sprint při vyčerpání energie
        public boolean stopSprintWhenEmpty = true;
    }
}

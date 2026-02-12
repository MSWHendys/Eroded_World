package cz.mcsworld.eroded.config.debug;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "ErodedWorld/debug")
public class DebugConfig implements ConfigData {
    public static DebugConfig get() {
        return AutoConfig
                .getConfigHolder(DebugConfig.class)
                .getConfig();
    }
    public boolean logLifecycle = true;
    public boolean logTerritory = false;
    public boolean logDarkness = false;
    public boolean logDeath = false;

    /**
     * Logování obtížnosti receptů (CG, multiplikátory)
     */
    @ConfigEntry.Gui.Tooltip
    public boolean logRecipeDifficulty = false;

    @ConfigEntry.Gui.CollapsibleObject
    public Darkness darkness = new Darkness();

    public static class Darkness {

        /**
         * Zobrazuje eye alpha (adaptace oka) v debug overlay
         */
        @ConfigEntry.Gui.Tooltip
        public boolean exposeEyeAlpha = false;

        /**
         * Zobrazuje lokální tmu (sampling světla) v debug overlay
         */
        @ConfigEntry.Gui.Tooltip
        public boolean exposeLocalDarkness = false;
    }
    @ConfigEntry.Gui.CollapsibleObject
    public Ecosystem ecosystem = new Ecosystem();

    public static class Ecosystem {

        /**
         * Zobrazuje debug zprávy o degradaci / regeneraci ekosystému
         */
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = false;
    }
}

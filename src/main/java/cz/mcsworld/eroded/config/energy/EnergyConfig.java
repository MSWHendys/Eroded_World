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

    @ConfigEntry.Gui.Tooltip
    public int numberEnergyFlashes = 10;

    @ConfigEntry.Gui.Tooltip
    public int maxEnergy = 100;

    @ConfigEntry.Gui.Tooltip
    public int energyPerSegment = 1;

    @ConfigEntry.Gui.Tooltip
    public int regenIntervalSeconds = 15;

    @ConfigEntry.Gui.Tooltip
    public boolean passiveRegenEnabled = true;

    @ConfigEntry.Gui.Tooltip
    public boolean sleepRestoresFull = true;

    @ConfigEntry.Gui.Tooltip
    public int tiredBelowSegments = 54;

    @ConfigEntry.Gui.Tooltip
    public int exhaustedBelowSegments = 34;

    @ConfigEntry.Gui.Tooltip
    public int exhaustedBelowEnergy = 24;

    @ConfigEntry.Gui.Tooltip
    public int emptyBelowSegments = 0;

    @ConfigEntry.Gui.Tooltip
    public int blinkBelowSegments = 2;

    @ConfigEntry.Gui.Tooltip
    public boolean blockWorkAtZero = true;

    @ConfigEntry.Gui.Tooltip
    public boolean warningsEnabled = true;

    @ConfigEntry.Gui.Tooltip
    public int warningCooldownMs = 1500;


    @ConfigEntry.Gui.Tooltip
    public int foodRestoreSnack = 2;

    @ConfigEntry.Gui.Tooltip
    public int foodRestoreSimple = 5;

    @ConfigEntry.Gui.Tooltip
    public int foodRestoreMeal = 10;

    @ConfigEntry.Gui.Tooltip
    public int foodRestoreFeast = 18;

    @ConfigEntry.Gui.Tooltip
    public boolean energyHudEnabled = true;

    @ConfigEntry.Gui.Tooltip
    public boolean showHudWhenFull = false;

    @ConfigEntry.Gui.Tooltip
    public int collapseDelayMs = 5000;


    @ConfigEntry.Gui.Tooltip
    public int WarningMessageTime = 60;


}

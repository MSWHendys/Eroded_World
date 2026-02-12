package cz.mcsworld.eroded.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "eroded")
public class ErodedConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public int darknessMaxAlpha = 220;

    @ConfigEntry.Gui.Tooltip
    public float darknessFadeSpeed = 0.08f;

    @ConfigEntry.Gui.Tooltip
    public boolean darknessVignetteEnabled = false;

    @ConfigEntry.Gui.Tooltip
    public int darknessVignetteMaxAlpha = 90;

    @ConfigEntry.Gui.Tooltip
    public float darknessVignetteSize = 0.18f;




}

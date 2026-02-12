package cz.mcsworld.eroded.config.territory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "ErodedWorld/territory")
public class TerritoryConfig implements ConfigData {
    public static TerritoryConfig get() {
        return AutoConfig
                .getConfigHolder(TerritoryConfig.class)
                .getConfig();
    }
    public int miningThreshold = 300;

    public float caveCollapseChanceLow = 0.005f;
    public float caveCollapseChanceMid = 0.02f;
    public float caveCollapseChanceHigh = 0.05f;

    public float mobSpawnChance = 0.25f;
}

package cz.mcsworld.eroded.world.territory;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;

public final class TerritoryThreatResolver {

    private TerritoryThreatResolver() {}

    public static float computeThreat(TerritoryCell cell, long tick) {
        var cfg = TerritoryConfig.get().server;

        int mining    = cell.getMining(tick);
        int pollution = cell.getPollution(tick);
        int forest    = cell.getForestation(tick);

        float miningN    = clamp(mining / 100.0f, 0.0f, 1.0f);
        float pollutionN = clamp(pollution / 100.0f, 0.0f, 1.0f);
        float forestN    = clamp(forest / 100.0f, 0.0f, 1.0f);

        float threat =
                (miningN * cfg.miningWeight) +
                        (pollutionN * cfg.pollutionWeight) -
                        (forestN * cfg.forestWeight);

        float scoreN = clamp(cell.getMiningScore() / 300.0f, 0.0f, 1.0f);
        threat += scoreN * 0.25f;

        return clamp(threat, 0.0f, 1.0f);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}

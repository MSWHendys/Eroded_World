package cz.mcsworld.eroded.world.territory;

public final class TerritoryThreatResolver {

    private TerritoryThreatResolver() {}

    public static float computeThreat(TerritoryData data, long tick) {

        int mining     = data.getMining(tick);
        int pollution  = data.getPollution(tick);
        int forest     = data.getForestation(tick);


        float miningN    = clamp(mining    / 100.0f, 0.0f, 1.0f);
        float pollutionN = clamp(pollution / 100.0f, 0.0f, 1.0f);
        float forestN    = clamp(forest    / 100.0f, 0.0f, 1.0f);

        float threat =
                (miningN    * 0.45f) +
                        (pollutionN * 0.45f) -
                        (forestN    * 0.30f);

        return clamp(threat, 0.0f, 1.0f);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}

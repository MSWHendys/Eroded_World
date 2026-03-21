package cz.mcsworld.eroded.client.data;

import cz.mcsworld.eroded.network.TerritoryDebugPacket;

public final class TerritoryClientDebugData {
    public static int miningBlocks, mining, pollution, forest;
    public static float threat;

    public static void update(TerritoryDebugPacket p) {
        miningBlocks = p.miningBlocks();
        mining = p.mining();
        pollution = p.pollution();
        forest = p.forest();
        threat = p.threat();
    }
}
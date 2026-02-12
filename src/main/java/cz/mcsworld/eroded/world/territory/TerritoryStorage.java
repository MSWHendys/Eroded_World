package cz.mcsworld.eroded.world.territory;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Map;

public final class TerritoryStorage {

    private static final Map<ServerWorld, Map<Long, TerritoryData>> DATA = new HashMap<>();

    private TerritoryStorage() {}

    public static TerritoryData get(ServerWorld world, ChunkPos pos) {
        Map<Long, TerritoryData> worldData =
                DATA.computeIfAbsent(world, w -> new HashMap<>());

        return worldData.computeIfAbsent(
                pos.toLong(),
                k -> new TerritoryData()
        );
    }

    public static void clearWorld(ServerWorld world) {
        DATA.remove(world);
    }
    public static void clearAll() {
        DATA.clear();
    }
}

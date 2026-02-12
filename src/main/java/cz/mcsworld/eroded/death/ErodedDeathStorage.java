package cz.mcsworld.eroded.death;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ErodedDeathStorage {

    private static final Map<UUID, ErodedDeathMemory> DATA =
            new ConcurrentHashMap<>();

    private ErodedDeathStorage() {}

    public static void put(UUID playerId, ErodedDeathMemory memory) {
        DATA.put(playerId, memory);
    }

    public static ErodedDeathMemory get(UUID playerId) {
        return DATA.get(playerId);
    }

    public static void clear(UUID playerId) {
        DATA.remove(playerId);
    }

    public static boolean putIfMoreValuable(
            UUID playerId,
            ErodedDeathMemory newMemory
    ) {
        ErodedDeathMemory current = DATA.get(playerId);

        if (current == null) {
            DATA.put(playerId, newMemory);
            return true;
        }

        if (current.isResolved()) {
            DATA.put(playerId, newMemory);
            return true;
        }

        if (newMemory.getValue() > current.getValue()) {
            DATA.put(playerId, newMemory);
            return true;
        }

        return false;
    }

}
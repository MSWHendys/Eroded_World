package cz.mcsworld.eroded.crafting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Runtime-only anti-spam state for crafting feedback. */
public final class CraftingMessageCooldown {

    private static final Map<UUID, Long> LAST_MESSAGE = new HashMap<>();

    private CraftingMessageCooldown() {}

    public static boolean tryAcquire(UUID playerId, long nowMs, long cooldownMs) {
        long last = LAST_MESSAGE.getOrDefault(playerId, 0L);
        if (nowMs - last < cooldownMs) {
            return false;
        }

        LAST_MESSAGE.put(playerId, nowMs);
        return true;
    }

    public static void cleanup(UUID playerId) {
        LAST_MESSAGE.remove(playerId);
    }
}

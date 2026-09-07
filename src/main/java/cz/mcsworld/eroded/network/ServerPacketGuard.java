package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.screen.TerritoryModuleScreenHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Small server-thread packet guard for C2S actions exposed by Eroded World.
 *
 * Fabric's object payload receivers run on the logical server thread, so this
 * intentionally uses a plain HashMap. The guard is not a punishment system; it
 * simply drops abusive/redundant requests before they can trigger world scans,
 * connected-claim traversals or collision checks.
 */
public final class ServerPacketGuard {

    private record Key(UUID playerId, String channel) {}
    private record Window(long second, int count) {}

    private static final Map<Key, Window> WINDOWS = new HashMap<>();

    private ServerPacketGuard() {}

    public static boolean allow(ServerPlayer player, String channel, int maxPerSecond) {
        if (player == null || player.hasDisconnected() || maxPerSecond <= 0) {
            return false;
        }

        long second = player.getServer().getTickCount() / 20L;
        Key key = new Key(player.getUUID(), channel);
        Window current = WINDOWS.get(key);

        if (current == null || current.second() != second) {
            WINDOWS.put(key, new Window(second, 1));
            return true;
        }

        if (current.count() >= maxPerSecond) {
            return false;
        }

        WINDOWS.put(key, new Window(second, current.count() + 1));
        return true;
    }

    /**
     * Territory-management packets are valid only for the menu the server
     * actually opened for the player. This prevents remote claim management by
     * forging an arbitrary anchor position and also gives us the normal 8-block
     * menu distance validation for every mutation packet.
     */
    public static boolean validTerritoryMenu(ServerPlayer player, BlockPos anchorPos) {
        if (player == null || anchorPos == null) {
            return false;
        }

        if (!(player.containerMenu instanceof TerritoryModuleScreenHandler menu)) {
            return false;
        }

        return menu.getAnchorPos().equals(anchorPos) && menu.stillValid(player);
    }

    public static void cleanup(UUID playerId) {
        if (playerId == null || WINDOWS.isEmpty()) {
            return;
        }

        Iterator<Key> iterator = WINDOWS.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().playerId().equals(playerId)) {
                iterator.remove();
            }
        }
    }
}

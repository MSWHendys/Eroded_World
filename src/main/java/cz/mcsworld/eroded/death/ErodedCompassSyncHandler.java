package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.network.ErodedCompassSyncPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Change-cached S2C state for the death compass.
 *
 * The client already counts remaining ticks down locally.  Older builds sent
 * the same payload every server tick even though ErodedDeathMemory rounds the
 * remaining value to whole seconds.  Cache the wire state so a stable compass
 * sends at most one update per second and the inactive state is sent only on a
 * transition.
 */
public final class ErodedCompassSyncHandler {

    private record LastSync(boolean active, long remainingTicks, long targetPos) {}

    private static final Map<UUID, LastSync> LAST_SENT = new HashMap<>();

    private ErodedCompassSyncHandler() {}

    public static void sync(ServerPlayer player) {
        ErodedDeathMemory mem = ErodedDeathStorage.get(player.getUUID());

        if (mem == null) {
            sendIfChanged(player, new LastSync(false, 0, 0));
            return;
        }

        long now = player.level().getServer().getTickCount();

        if (mem.isExpired(now) || mem.isResolved()) {
            sendIfChanged(player, new LastSync(false, 0, 0));
            return;
        }

        BlockPos targetPos = ErodedCompassTargetResolver.resolveTarget(player, mem);
        sendIfChanged(
                player,
                new LastSync(true, mem.getRemainingTicks(now), targetPos.asLong())
        );
    }

    public static void forceSync(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
        sync(player);
    }

    public static void cleanup(UUID playerId) {
        LAST_SENT.remove(playerId);
    }

    private static void sendIfChanged(ServerPlayer player, LastSync next) {
        LastSync previous = LAST_SENT.get(player.getUUID());
        if (next.equals(previous)) {
            return;
        }

        LAST_SENT.put(player.getUUID(), next);
        SafeNetworkUtil.safeSend(
                player,
                new ErodedCompassSyncPacket(
                        next.active(),
                        next.remainingTicks(),
                        next.targetPos()
                )
        );
    }
}

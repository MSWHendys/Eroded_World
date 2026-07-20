package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.network.ErodedCompassSyncPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class ErodedCompassSyncHandler {

    private ErodedCompassSyncHandler() {}

    public static void sync(ServerPlayer player) {

        ErodedDeathMemory mem =
                ErodedDeathStorage.get(player.getUUID());

        if (mem == null) {
            SafeNetworkUtil.safeSend(
                    player,
                    new ErodedCompassSyncPacket(false, 0, 0)
            );
            return;
        }

        long now = player.level().getServer().getTickCount();

        if (mem.isExpired(now) || mem.isResolved()) {
            SafeNetworkUtil.safeSend(
                    player,
                    new ErodedCompassSyncPacket(false, 0, 0)
            );
            return;
        }

        BlockPos targetPos = ErodedCompassTargetResolver.resolveTarget(player, mem);

        SafeNetworkUtil.safeSend(
                player,
                new ErodedCompassSyncPacket(
                        true,
                        mem.getRemainingTicks(now),
                        targetPos.asLong()
                )
        );
    }
}
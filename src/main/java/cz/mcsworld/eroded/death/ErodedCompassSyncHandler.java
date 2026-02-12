package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.network.ErodedCompassSyncPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class ErodedCompassSyncHandler {

    private ErodedCompassSyncHandler() {}

    public static void sync(ServerPlayerEntity player) {

        ErodedDeathMemory mem =
                ErodedDeathStorage.get(player.getUuid());

        if (mem == null) {
            SafeNetworkUtil.safeSend(
                    player,
                    new ErodedCompassSyncPacket(false, 0, 0)
            );
            return;
        }

        long now = player.getServer().getTicks();

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
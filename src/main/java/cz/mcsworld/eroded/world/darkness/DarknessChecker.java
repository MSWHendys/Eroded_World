package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.network.SafeNetworkUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import cz.mcsworld.eroded.network.DarknessStatePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DarknessChecker {

    private static final Map<UUID, Boolean> LAST_STATE = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    private DarknessChecker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DarknessChecker::onTick);
    }

    private static void onTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 20 != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean inDarkness = isInDangerDarkness(player);

            UUID id = player.getUUID();
            boolean last = LAST_STATE.getOrDefault(id, false);
            if (inDarkness == last) continue;

            LAST_STATE.put(id, inDarkness);

            SafeNetworkUtil.safeSend(
                    player,
                    new DarknessStatePacket(inDarkness)
            );
        }
    }

    public static boolean isInDangerDarkness(ServerPlayer player) {
        var world = player.level();
        var pos = player.blockPosition();

        int block = world.getBrightness(LightLayer.BLOCK, pos);
        int sky   = world.getBrightness(LightLayer.SKY, pos);

        int total = Math.max(block, sky);
        return total < 3;
    }

    public static boolean isInDangerDarkness(
            net.minecraft.server.level.ServerLevel world,
            net.minecraft.core.BlockPos pos
    ) {
        int block = world.getBrightness(LightLayer.BLOCK, pos);
        int sky   = world.getBrightness(LightLayer.SKY, pos);

        int total = Math.max(block, sky);
        return total < 3;
    }
    public static void cleanup(UUID playerId) {
        if (LAST_STATE.containsKey(playerId)) {
            LAST_STATE.remove(playerId);
        }
    }
}

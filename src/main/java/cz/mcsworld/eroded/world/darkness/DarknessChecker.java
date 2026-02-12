package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.network.SafeNetworkUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.LightType;
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

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            boolean inDarkness = isInDangerDarkness(player);

            UUID id = player.getUuid();
            boolean last = LAST_STATE.getOrDefault(id, false);
            if (inDarkness == last) continue;

            LAST_STATE.put(id, inDarkness);

            SafeNetworkUtil.safeSend(
                    player,
                    new DarknessStatePacket(inDarkness)
            );
        }
    }

    public static boolean isInDangerDarkness(ServerPlayerEntity player) {
        var world = player.getWorld();
        var pos = player.getBlockPos();

        int block = world.getLightLevel(LightType.BLOCK, pos);
        int sky   = world.getLightLevel(LightType.SKY, pos);

        int total = Math.max(block, sky);
        return total < 3;
    }

    public static boolean isInDangerDarkness(
            net.minecraft.server.world.ServerWorld world,
            net.minecraft.util.math.BlockPos pos
    ) {
        int block = world.getLightLevel(LightType.BLOCK, pos);
        int sky   = world.getLightLevel(LightType.SKY, pos);

        int total = Math.max(block, sky);
        return total < 3;
    }
}

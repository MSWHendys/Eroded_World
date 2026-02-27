package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.network.EnergySyncPacket;
import cz.mcsworld.eroded.network.EnergyWarningPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnergySyncHandler {

    private static final Map<UUID, Integer> LAST_SENT = new ConcurrentHashMap<>();

    private EnergySyncHandler() {}

    public static void register() {

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                forceSync(handler.getPlayer())
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LAST_SENT.remove(handler.getPlayer().getUuid())
        );

        ServerTickEvents.END_SERVER_TICK.register(EnergySyncHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        var root = EnergyConfig.get();
        var cfg = root.server.warnings;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            boolean changed = syncIfChanged(player);

            if (!cfg.warningsEnabled) continue;
            if (!changed) continue;

            SkillData data = SkillManager.get(player);
            SkillData.EnergyState worse = data.detectWorseningState();
            if (worse == null) continue;

            SafeNetworkUtil.safeSend(player, new EnergyWarningPacket(worse));
        }
    }

    public static boolean syncIfChanged(ServerPlayerEntity player) {
        SkillData data = SkillManager.get(player);
        int energy = data.getEnergy();

        int last = LAST_SENT.getOrDefault(player.getUuid(), Integer.MIN_VALUE);
        if (energy == last) return false;

        LAST_SENT.put(player.getUuid(), energy);
        SafeNetworkUtil.safeSend(player, new EnergySyncPacket(energy));
        return true;
    }

    public static void forceSync(ServerPlayerEntity player) {
        SkillData data = SkillManager.get(player);
        int energy = data.getEnergy();

        LAST_SENT.put(player.getUuid(), energy);
        SafeNetworkUtil.safeSend(player, new EnergySyncPacket(energy));
    }
}
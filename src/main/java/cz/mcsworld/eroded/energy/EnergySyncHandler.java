package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.network.EnergyWarningPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnergySyncHandler {

    private record LastEnergySync(int energy, int immunitySeconds) {
    }

    private static final Map<UUID, LastEnergySync> LAST_SENT = new ConcurrentHashMap<>();

    private EnergySyncHandler() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            SkillData data = SkillManager.get(player);

            var state = EnergyPersistentState.get(server.overworld());
            int saved = state.getEnergy(player.getUUID(), data.getMaxEnergy());

            data.setEnergy(saved);
            forceSync(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LAST_SENT.remove(handler.getPlayer().getUUID())
        );

        ServerTickEvents.END_SERVER_TICK.register(EnergySyncHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        var root = EnergyConfig.get();
        var cfg = root.server.warnings;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            boolean changed = syncIfChanged(player);

            if (!cfg.warningsEnabled) continue;
            if (!changed) continue;

            SkillData data = SkillManager.get(player);
            SkillData.EnergyState worse = data.detectWorseningState();
            if (worse == null) continue;

            SafeNetworkUtil.safeSend(player, new EnergyWarningPacket(worse));
        }
    }

    public static boolean syncIfChanged(ServerPlayer player) {
        SkillData data = SkillManager.get(player);

        int energy = data.getEnergy();
        int currentImmunity = (int) (data.getImmunityRemainingMs() / 1000);

        LastEnergySync current = new LastEnergySync(energy, currentImmunity);
        LastEnergySync last = LAST_SENT.get(player.getUUID());

        if (current.equals(last)) {
            return false;
        }

        LAST_SENT.put(player.getUUID(), current);

        var state = EnergyPersistentState.get(player.getServer().overworld());
        state.setEnergy(player.getUUID(), energy);

        SkillManager.sync(player);
        return true;
    }

    public static void forceSync(ServerPlayer player) {
        SkillData data = SkillManager.get(player);

        int energy = data.getEnergy();
        int currentImmunity = (int) (data.getImmunityRemainingMs() / 1000);

        LAST_SENT.put(
                player.getUUID(),
                new LastEnergySync(energy, currentImmunity)
        );

        SkillManager.sync(player);
    }
}
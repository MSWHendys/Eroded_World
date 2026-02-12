package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.network.EnergySyncPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.util.ActionBarUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        EnergyConfig cfg = EnergyConfig.get();
        if (!cfg.warningsEnabled) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            SkillData data = SkillManager.get(player);

            int currentEnergy = data.getEnergy();
            int lastEnergy = LAST_SENT.getOrDefault(player.getUuid(), currentEnergy);

            boolean regenerating = currentEnergy > lastEnergy;

            syncIfChanged(player);

            /*SkillData.EnergyState state = data.updateEnergyState(regenerating);
            if (state == null) continue;

            String key = switch (state) {
                case TIRED -> "eroded.energy.state.tired";
                case EXHAUSTED -> "eroded.energy.state.exhausted";
                case EMPTY -> "eroded.energy.state.empty";
                default -> null;
            };

            if (key != null) {
                ActionBarUtil.send(player, Text.translatable(key));
            }*/
        }

    }

    public static void syncIfChanged(ServerPlayerEntity player) {
        SkillData data = SkillManager.get(player);
        int energy = data.getEnergy();

        int last = LAST_SENT.getOrDefault(player.getUuid(), Integer.MIN_VALUE);
        if (energy == last) return;

        LAST_SENT.put(player.getUuid(), energy);
        SafeNetworkUtil.safeSend(player, new EnergySyncPacket(energy));
    }

    public static void forceSync(ServerPlayerEntity player) {
        SkillData data = SkillManager.get(player);
        int energy = data.getEnergy();

        LAST_SENT.put(player.getUuid(), energy);
        SafeNetworkUtil.safeSend(player, new EnergySyncPacket(energy));
    }
}

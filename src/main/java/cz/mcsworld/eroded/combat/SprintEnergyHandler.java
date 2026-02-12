package cz.mcsworld.eroded.combat;

import cz.mcsworld.eroded.network.EnergyWarningPacket;
import cz.mcsworld.eroded.network.EnergySyncPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SprintEnergyHandler {

    private static final int DRAIN_INTERVAL_TICKS = 10;
    private static final int ENERGY_PER_INTERVAL = 1;
    private static final int MIN_ENERGY_TO_SPRINT = 2;

    private static final Map<UUID, Integer> sprintTicks = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(SprintEnergyHandler::onWorldTick);
    }

    private static void onWorldTick(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {

            if (!player.isAlive()) {
                sprintTicks.remove(player.getUuid());
                continue;
            }

            if (player.getAbilities().creativeMode
                    || player.getAbilities().flying
                    || player.isSpectator()
                    || player.isGliding()) {

                sprintTicks.remove(player.getUuid());
                continue;
            }

            UUID id = player.getUuid();
            SkillData data = SkillManager.get(player);

            if (!player.isSprinting()) {
                sprintTicks.remove(id);
                continue;
            }

            if (data.getEnergy() < MIN_ENERGY_TO_SPRINT) {
                if (!player.isUsingItem()) {
                    player.setSprinting(false);
                }
                sprintTicks.remove(id);

                SafeNetworkUtil.safeSend(player, new EnergySyncPacket(data.getEnergy()));
                SafeNetworkUtil.safeSend(player, new EnergyWarningPacket());
                continue;
            }

            int ticks = sprintTicks.getOrDefault(id, 0) + 1;

            if (ticks >= DRAIN_INTERVAL_TICKS) {
                data.consumeEnergy(ENERGY_PER_INTERVAL);
                ticks = 0;

                SafeNetworkUtil.safeSend(player, new EnergySyncPacket(data.getEnergy()));
            }

            sprintTicks.put(id, ticks);
        }
    }

}
package cz.mcsworld.eroded.combat;

import cz.mcsworld.eroded.config.combat.CombatConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SprintEnergyHandler {

    private static final Map<UUID, Integer> sprintTicks = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(SprintEnergyHandler::onWorldTick);
    }

    private static void onWorldTick(ServerLevel world) {
        if (!EnergyConfig.get().server.enabled) {
            for (ServerPlayer player : world.players()) {
                sprintTicks.remove(player.getUUID());
            }
            return;
        }

        CombatConfig root = CombatConfig.get();
        if (!root.enabled || !root.sprint.enabled) return;
        CombatConfig.Sprint cfg = root.sprint;

        for (ServerPlayer player : world.players()) {

            if (!player.isAlive()) {
                sprintTicks.remove(player.getUUID());
                continue;
            }

            if (player.getAbilities().instabuild
                    || player.getAbilities().flying
                    || player.isSpectator()
                    || player.isFallFlying()) {

                sprintTicks.remove(player.getUUID());
                continue;
            }

            UUID id = player.getUUID();
            SkillData data = SkillManager.get(player);

            if (!player.isSprinting()) {
                sprintTicks.remove(id);
                continue;
            }

            int currentEnergy = data.getEnergy();

            if (currentEnergy < cfg.minEnergyToSprint) {
                if (cfg.stopSprintWhenEmpty) {
                    player.setSprinting(false);
                }

                sprintTicks.remove(id);
                continue;
            }

            int ticks = sprintTicks.getOrDefault(id, 0) + 1;

            if (ticks >= cfg.drainIntervalTicks) {
                data.consumeEnergy(cfg.energyPerInterval);
                SkillManager.save(player);
                ticks = 0;
            }

            sprintTicks.put(id, ticks);

        }
    }

    public static void cleanup(UUID playerId) {
        sprintTicks.remove(playerId);
    }
}

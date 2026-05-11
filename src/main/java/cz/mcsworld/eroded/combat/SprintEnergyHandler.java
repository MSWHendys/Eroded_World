package cz.mcsworld.eroded.combat;

import cz.mcsworld.eroded.config.combat.CombatConfig;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SprintEnergyHandler {

    private static final Map<UUID, Integer> sprintTicks = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(SprintEnergyHandler::onWorldTick);
    }

    private static void onWorldTick(ServerWorld world) {
        CombatConfig root = CombatConfig.get();
        if (!root.enabled || !root.sprint.enabled) return;
        CombatConfig.Sprint cfg = root.sprint;

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

            int currentEnergy = data.getEnergy();

            if (currentEnergy < cfg.minEnergyToSprint) {
                if (cfg.stopSprintWhenEmpty) {
                    player.setSprinting(false);
                }

                sprintTicks.remove(id);

                SkillManager.sync(player);
                continue;
            }

            int ticks = sprintTicks.getOrDefault(id, 0) + 1;

            if (ticks >= cfg.drainIntervalTicks) {
                data.consumeEnergy(cfg.energyPerInterval);
                SkillManager.save(player);
                ticks = 0;
            }

            sprintTicks.put(id, ticks);

            if (world.getTime() % 20 == 0 && data.isImmune()) {
                SkillManager.sync(player);
            }
        }
    }

    public static void cleanup(UUID playerId) {
        sprintTicks.remove(playerId);
    }
}
package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public final class DeathRespawnHandler {


    private static final float ENERGY_AFTER_DEATH_RATIO = 0.55F;

    private DeathRespawnHandler() {
    }

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register(DeathRespawnHandler::onRespawn);
    }

    private static void onRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        newPlayer.removeStatusEffect(StatusEffects.SLOWNESS);

        if (alive) {
            return;
        }

        applyDeathEnergyPenalty(newPlayer);
        RespawnProtectionManager.start(newPlayer);
    }

    private static void applyDeathEnergyPenalty(ServerPlayerEntity player) {
        SkillData data = SkillManager.get(player);

        data.setEnergyAfterDeath(ENERGY_AFTER_DEATH_RATIO);

        SkillManager.save(player);
        SkillManager.sync(player);
    }
}
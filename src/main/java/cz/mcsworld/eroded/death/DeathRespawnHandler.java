package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

public final class DeathRespawnHandler {


    private static final float ENERGY_AFTER_DEATH_RATIO = 0.55F;

    private DeathRespawnHandler() {
    }

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register(DeathRespawnHandler::onRespawn);
    }

    private static void onRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        newPlayer.removeEffect(MobEffects.SLOWNESS);

        if (alive) {
            return;
        }

        applyDeathEnergyPenalty(newPlayer);
        RespawnProtectionManager.start(newPlayer);
    }

    private static void applyDeathEnergyPenalty(ServerPlayer player) {
        SkillData data = SkillManager.get(player);

        data.setEnergyAfterDeath(ENERGY_AFTER_DEATH_RATIO);

        SkillManager.save(player);
        SkillManager.sync(player);
    }
}
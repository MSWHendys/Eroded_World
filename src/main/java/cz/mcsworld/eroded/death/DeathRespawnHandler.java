package cz.mcsworld.eroded.death;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public final class DeathRespawnHandler {

    private DeathRespawnHandler() {}

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register(
                DeathRespawnHandler::onRespawn
        );
    }

    private static void onRespawn(
            ServerPlayerEntity oldPlayer,
            ServerPlayerEntity newPlayer,
            boolean alive
    ) {
        newPlayer.removeStatusEffect(StatusEffects.SLOWNESS);
    }
}

package cz.mcsworld.eroded.world.spawn;

import cz.mcsworld.eroded.server.spawn.SpawnProtectionSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.level.Level;

public final class SpawnProtectionTicker {

    private SpawnProtectionTicker() {}

    public static void register() {

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                SpawnProtectionSystem.clearLegacyPlayerInvulnerability(handler.getPlayer())
        );

        ServerTickEvents.END_WORLD_TICK.register(world -> {

            if (world.dimension() != Level.OVERWORLD) return;
            // Mob push/target cleanup is navigation work; 4 Hz is responsive
            // enough while avoiding a full spawn-area entity query every tick.
            if (world.getGameTime() % 5L != 0L) return;

            SpawnProtectionSystem.tick(world);
        });
    }
}

package cz.mcsworld.eroded.world.spawn;

import cz.mcsworld.eroded.server.spawn.SpawnProtectionSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.World;

public final class SpawnProtectionTicker {

    private SpawnProtectionTicker() {}

    public static void register() {

        ServerTickEvents.END_WORLD_TICK.register(world -> {

            if (world.getRegistryKey() != World.OVERWORLD) return;

            SpawnProtectionSystem.tick(world);
        });
    }
}

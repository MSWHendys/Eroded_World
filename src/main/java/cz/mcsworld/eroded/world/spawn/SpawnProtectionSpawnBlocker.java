package cz.mcsworld.eroded.world.spawn;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;

public final class SpawnProtectionSpawnBlocker {

    private SpawnProtectionSpawnBlocker() {}

    public static void register() {

        ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerLevel world) -> {

            var cfg = TerritoryConfig.get().server;

            if (!cfg.enabled || !cfg.spawnProtectionEnabled) {
                return;
            }

            if (!(entity instanceof Enemy)) {
                return;
            }

            BlockPos spawn = world.getRespawnData().pos();

            int radius = cfg.spawnProtectionRadius;
            int radiusSq = radius * radius;

            BlockPos pos = entity.blockPosition();

            int dx = pos.getX() - spawn.getX();
            int dz = pos.getZ() - spawn.getZ();

            if (dx * dx + dz * dz <= radiusSq) {

                entity.discard();

            }
        });
    }
}
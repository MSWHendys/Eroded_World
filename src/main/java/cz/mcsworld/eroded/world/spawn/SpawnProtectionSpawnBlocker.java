package cz.mcsworld.eroded.world.spawn;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;

import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.math.BlockPos;

public final class SpawnProtectionSpawnBlocker {

    private SpawnProtectionSpawnBlocker() {}

    public static void register() {

        ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerWorld world) -> {

            var cfg = TerritoryConfig.get().server;

            if (!cfg.enabled || !cfg.spawnProtectionEnabled) {
                return;
            }

            if (!(entity instanceof Monster)) {
                return;
            }

            BlockPos spawn = world.getSpawnPos();

            int radius = cfg.spawnProtectionRadius;
            int radiusSq = radius * radius;

            BlockPos pos = entity.getBlockPos();

            int dx = pos.getX() - spawn.getX();
            int dz = pos.getZ() - spawn.getZ();

            if (dx * dx + dz * dz <= radiusSq) {

                entity.discard();

            }
        });
    }
}
package cz.mcsworld.eroded.entity;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;

/**
 * Explicit lifecycle for Eroded mobs spawned by the territory system.
 *
 * Territory mobs have a custom display name, which makes vanilla consider them
 * custom-persistent even if setPersistenceRequired() is not used. Therefore the
 * territory lifecycle must own despawning explicitly.
 */
public final class ErodedMobDespawnBehaviour {

    public static final String TAG_TERRITORY_SPAWN = "eroded_territory_spawn";

    private ErodedMobDespawnBehaviour() {
    }

    /**
     * Handles despawn for a territory-managed mob.
     *
     * @return true when vanilla despawn handling must be skipped for this mob;
     *         false when this is not a territory-managed mob and vanilla should
     *         handle despawn normally.
     */
    public static boolean handleDespawn(Mob mob) {
        boolean territorySpawn = mob.getTags().contains(TAG_TERRITORY_SPAWN);

        // Migration for mobs saved by versions before Part 6. TerritoryMobSpawnHandler
        // was the only code path that called setPersistenceRequired() for Eroded mobs.
        boolean legacyTerritorySpawn = !territorySpawn
                && mob.isPersistenceRequired()
                && mob.getTags().contains(ErodedMobSunBehaviour.TAG_ERODED);

        if (!territorySpawn && !legacyTerritorySpawn) {
            return false;
        }

        if (legacyTerritorySpawn) {
            mob.addTag(TAG_TERRITORY_SPAWN);
        }

        if (!(mob.level() instanceof ServerLevel world)) {
            return true;
        }

        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            mob.discard();
            return true;
        }

        int radius = Math.max(1, TerritoryConfig.get().server.mobDespawnRadius);
        double radiusSq = (double) radius * radius;

        for (ServerPlayer player : world.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            if (mob.distanceToSqr(player) <= radiusSq) {
                // Territory mobs should remain stable while an active player is
                // inside the configured lifecycle radius. Their custom name would
                // otherwise interact with vanilla persistence rules.
                mob.setNoActionTime(0);
                return true;
            }
        }

        mob.discard();
        return true;
    }
}

package cz.mcsworld.eroded.server.spawn;

import cz.mcsworld.eroded.core.EntityInvulnerabilityCompat;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpawnProtectionSystem {

    private SpawnProtectionSystem() {}

    public static void tick(ServerLevel world) {
        var root = TerritoryConfig.get();
        var cfg = root.server;
        if (!cfg.enabled || !cfg.spawnProtectionEnabled) return;

        int radius = Math.max(0, cfg.spawnProtectionRadius);
        int radiusSq = radius * radius;
        int checkRange = radius + 24;

        BlockPos spawn = world.getRespawnData().pos();

        AABB spawnBox = new AABB(spawn).inflate(checkRange);

        for (Mob mob : world.getEntitiesOfClass(
                Mob.class,
                spawnBox,
                e -> e.isAlive() && e instanceof Enemy)) {

            if (isInSpawn(mob.blockPosition(), spawn, radiusSq)) {

                pushMobOut(mob, spawn, radius);
            } else {

                LivingEntity target = mob.getTarget();

                if (target instanceof ServerPlayer targetPlayer) {
                    if (isInSpawn(targetPlayer.blockPosition(), spawn, radiusSq)) {

                        mob.setTarget(null);
                        mob.setAggressive(false);

                        if (mob instanceof NeutralMob angerable) {
                            angerable.stopBeingAngry();
                        }
                    }
                }
            }
        }
    }

    private static boolean isInSpawn(BlockPos pos, BlockPos spawn, int radiusSq) {
        return pos.distSqr(spawn) <= radiusSq;
    }

    private static void pushMobOut(Mob mob, BlockPos spawn, int radius) {

        Vec3 mobPos = mob.position();
        Vec3 center = Vec3.atCenterOf(spawn);
        Vec3 direction = mobPos.subtract(center);

        if (direction.lengthSqr() == 0) {
            direction = new Vec3(1, 0, 0);
        }

        Vec3 normalized = direction.normalize();

        Vec3 escapePos = center.add(normalized.scale(radius + 6));

        mob.setTarget(null);
        mob.setAggressive(false);

        mob.getNavigation().moveTo(
                escapePos.x,
                escapePos.y,
                escapePos.z,
                1.2D
        );
    }

    /**
     * Damage protection for the world spawn.
     *
     * This deliberately does not use Entity#setInvulnerable. The old implementation
     * stored a persistent entity flag which could survive a dimension change or a
     * server restart. Damage is now decided from the player's current position at
     * the moment the hit is processed.
     */
    public static boolean shouldPreventDamage(ServerPlayer player) {
        var cfg = TerritoryConfig.get().server;

        if (!cfg.enabled || !cfg.spawnProtectionEnabled) return false;
        if (player.level().dimension() != Level.OVERWORLD) return false;
        if (player.isCreative() || player.isSpectator()) return false;

        ServerLevel world = (ServerLevel) player.level();
        int radius = Math.max(0, cfg.spawnProtectionRadius);
        int radiusSq = radius * radius;

        return isInSpawn(
                player.blockPosition(),
                world.getRespawnData().pos(),
                radiusSq
        );
    }

    public static boolean isPlayerInSpawn(ServerPlayer player) {
        var cfg = TerritoryConfig.get().server;
        if (!cfg.enabled || !cfg.spawnProtectionEnabled) return false;
        if (player.level().dimension() != Level.OVERWORLD) return false;

        ServerLevel world = (ServerLevel) player.level();
        int radius = Math.max(0, cfg.spawnProtectionRadius);
        int radiusSq = radius * radius;
        return isInSpawn(
                player.blockPosition(),
                world.getRespawnData().pos(),
                radiusSq);
    }

    /**
     * Migration helper for builds that used setInvulnerable(true) for spawn safety.
     * The flag is serialized in player data, so a leaked value can outlive the old
     * mod version. This method is only called on login and only for normal players.
     */
    public static void clearLegacyPlayerInvulnerability(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;
        if (!EntityInvulnerabilityCompat.isInvulnerable(player)) return;

        // The old implementation could legitimately leave this flag behind only
        // while the player was inside the Overworld spawn radius, or after the
        // player escaped to another dimension before the Overworld ticker reset it.
        // Avoid clearing unrelated/admin invulnerability for players already well
        // outside the spawn area in the Overworld.
        if (player.level().dimension() == Level.OVERWORLD) {
            var cfg = TerritoryConfig.get().server;
            ServerLevel world = (ServerLevel) player.level();
            int radius = Math.max(0, cfg.spawnProtectionRadius);
            int radiusSq = radius * radius;

            if (!isInSpawn(player.blockPosition(), world.getRespawnData().pos(), radiusSq)) {
                return;
            }
        }

        EntityInvulnerabilityCompat.clear(player);
    }
}

package cz.mcsworld.eroded.server.spawn;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpawnProtectionSystem {

    private SpawnProtectionSystem() {}

    public static void tick(ServerLevel world) {
        var root = TerritoryConfig.get();
        var cfg = root.server;
        if (!cfg.enabled || !cfg.spawnProtectionEnabled) return;

        int radius = cfg.spawnProtectionRadius;
        int radiusSq = radius * radius;
        int checkRange = radius + 24;

        BlockPos spawn = world.getRespawnData().pos();


        for (ServerPlayer player : world.players()) {

            if (!player.isCreative() && !player.isSpectator()) {
                boolean playerInSpawn = isInSpawn(player.blockPosition(), spawn, radiusSq);
                player.setInvulnerable(playerInSpawn);
            }
        }

        AABB spawnBox = new AABB(spawn).inflate(checkRange);

        for (Mob mob : world.getEntitiesOfClass(
                Mob.class,
                spawnBox,
                e -> e.isAlive() && e instanceof Enemy)) {

            if (isInSpawn(mob.blockPosition(), spawn,  radiusSq)) {

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

    public static boolean isPlayerInSpawn(ServerPlayer player) {
        var cfg = TerritoryConfig.get().server;
        if (!cfg.enabled || !cfg.spawnProtectionEnabled) return false;
        ServerLevel world = (ServerLevel) player.level();
        int radius = cfg.spawnProtectionRadius;
        int radiusSq = radius * radius;
        return isInSpawn(
                player.blockPosition(),
                world.getRespawnData().pos(),
                radiusSq);
    }
}
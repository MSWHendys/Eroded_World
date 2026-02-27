package cz.mcsworld.eroded.server.spawn;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class SpawnProtectionSystem {

    private SpawnProtectionSystem() {}

    public static void tick(ServerWorld world) {
        var root = TerritoryConfig.get();
        var cfg = root.server;
        if (!cfg.enabled || !cfg.spawnProtectionEnabled) return;

        int radius = cfg.spawnProtectionRadius;
        int radiusSq = radius * radius;
        int checkRange = radius + 24;

        BlockPos spawn = world.getSpawnPos();


        for (ServerPlayerEntity player : world.getPlayers()) {

            if (!player.isCreative() && !player.isSpectator()) {
                boolean playerInSpawn = isInSpawn(player.getBlockPos(), spawn, radiusSq);
                player.setInvulnerable(playerInSpawn);
            }
        }

        Box spawnBox = new Box(spawn).expand(checkRange);

        for (MobEntity mob : world.getEntitiesByClass(
                MobEntity.class,
                spawnBox,
                e -> e.isAlive() && e instanceof Monster)) {

            if (isInSpawn(mob.getBlockPos(), spawn,  radiusSq)) {

                pushMobOut(mob, spawn, radius);
            } else {

                LivingEntity target = mob.getTarget();

                if (target instanceof ServerPlayerEntity targetPlayer) {
                    if (isInSpawn(targetPlayer.getBlockPos(), spawn, radiusSq)) {

                        mob.setTarget(null);
                        mob.setAttacking(false);

                        if (mob instanceof Angerable angerable) {
                            angerable.stopAnger();
                        }
                    }
                }
            }
        }
    }

    private static boolean isInSpawn(BlockPos pos, BlockPos spawn, int radiusSq) {
        return pos.getSquaredDistance(spawn) <= radiusSq;
    }

    private static void pushMobOut(MobEntity mob, BlockPos spawn, int radius) {

        Vec3d mobPos = mob.getPos();
        Vec3d center = Vec3d.ofCenter(spawn);
        Vec3d direction = mobPos.subtract(center);

        if (direction.lengthSquared() == 0) {
            direction = new Vec3d(1, 0, 0);
        }

        Vec3d normalized = direction.normalize();

        Vec3d escapePos = center.add(normalized.multiply(radius + 6));

        mob.setTarget(null);
        mob.setAttacking(false);

        mob.getNavigation().startMovingTo(
                escapePos.x,
                escapePos.y,
                escapePos.z,
                1.2D
        );
    }

    public static boolean isPlayerInSpawn(ServerPlayerEntity player) {
        var cfg = TerritoryConfig.get().server;
        if (!cfg.enabled || !cfg.spawnProtectionEnabled) return false;
        ServerWorld world = (ServerWorld) player.getWorld();
        int radius = cfg.spawnProtectionRadius;
        int radiusSq = radius * radius;
        return isInSpawn(
                player.getBlockPos(),
                world.getSpawnPos(),
                radiusSq);
    }
}
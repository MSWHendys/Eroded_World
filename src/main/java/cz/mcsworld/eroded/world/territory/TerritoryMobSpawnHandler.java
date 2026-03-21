package cz.mcsworld.eroded.world.territory;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.darkness.MutatedMobResolver;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.List;

public final class TerritoryMobSpawnHandler {

    private static final String TAG_ERODED = "eroded_special_mob";
    private static final String TAG_PERMANENT = "eroded_sun_proof";
    private static final String TAG_TEMPORARY = "eroded_temp_proof";
    private static final String TAG_BURN_PREFIX = "burn_at_";

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(TerritoryMobSpawnHandler::onWorldTick);
    }

    private static void onWorldTick(ServerWorld world) {
        var cfg = TerritoryConfig.get().server;
        if (!cfg.enabled || !cfg.mobSpawnControlEnabled) return;

        if (world.getTime() % cfg.spawnCheckInterval == 0) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                trySpawnErodedMob(player, world, cfg);
            }
        }
        handleMobBehaviour(world, cfg);
    }

    private static void trySpawnErodedMob(ServerPlayerEntity player, ServerWorld world, TerritoryConfig.Server cfg) {
        BlockPos pPos = player.getBlockPos();

        if (cfg.spawnProtectionEnabled) {
            if (pPos.isWithinDistance(world.getSpawnPos(), cfg.spawnProtectionRadius)) return;
        }

        ChunkPos cp = new ChunkPos(pPos);
        TerritoryWorldState worldState = TerritoryWorldState.get(world);
        TerritoryCell cell = worldState.getOrCreateCell(TerritoryCellKey.fromChunk(cp.x, cp.z));

        int mined = cell.getMiningScore();
        if (mined < cfg.miningThreshold) return;

        BlockPos startPos = cp.getStartPos();
        int currentMobs = world.getEntitiesByClass(HostileEntity.class,
                new Box(startPos.getX(), -64, startPos.getZ(), startPos.getX() + 15, 320, startPos.getZ() + 15),
                e -> e.getCommandTags().contains(TAG_ERODED)).size();

        if (currentMobs >= cfg.mobMaxPerChunk) return;

        Random random = world.getRandom();
        float threat = TerritoryThreatResolver.computeThreat(cell, world.getTime());

        float spawnChance = Math.max(cfg.spawnKeepMinChance, threat * 0.4f);
        if (random.nextFloat() > spawnChance) return;

        int count = random.nextInt(cfg.maxMobsPerSpawnCycle + 1);

        for (int i = 0; i < count; i++) {
            if (currentMobs + i >= cfg.mobMaxPerChunk) break;

            BlockPos spawnPos = findSpawnPos(world, pPos, random, cfg);
            if (spawnPos == null) continue;

            EntityType<? extends HostileEntity> type = random.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
            HostileEntity mob = type.create(world, SpawnReason.EVENT);

            if (mob != null) {
                mob.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, random.nextFloat() * 360, 0);
                mob.setPersistent();
                mob.addCommandTag(TAG_ERODED);

                if (cfg.mobBuffEnabled && threat > cfg.mobBuffThreshold) {
                    mob.addCommandTag(MutatedMobResolver.MUTATED_TAG);
                }

                applyErodedStats(mob, mined, threat, cfg);
                applySpawnBehaviour(mob, random, world);

                world.spawnEntityAndPassengers(mob);
            }
        }
    }

    private static void applySpawnBehaviour(HostileEntity mob, Random random, ServerWorld world) {
        float r = random.nextFloat() * 100;
        if (r <= 20f) {
            mob.addCommandTag(TAG_PERMANENT);
        } else if (r <= 40f) {
            mob.addCommandTag(TAG_TEMPORARY);
            long burnTime = world.getTime() + 600 + random.nextInt(600);
            mob.addCommandTag(TAG_BURN_PREFIX + burnTime);
        }
    }

    private static void handleMobBehaviour(ServerWorld world, TerritoryConfig.Server cfg) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            Box box = new Box(player.getBlockPos()).expand(cfg.mobDespawnRadius);
            List<HostileEntity> nearby = world.getEntitiesByClass(HostileEntity.class, box,
                    e -> e.getCommandTags().contains(TAG_ERODED));

            for (HostileEntity mob : nearby) {
                processProtection(mob);
            }
        }
    }

    private static void processProtection(HostileEntity mob) {
        if (!mob.isAlive()) return;

        if (mob.getCommandTags().contains(TAG_PERMANENT)) {
            if (mob.isOnFire()) mob.extinguish();
        } else if (mob.getCommandTags().contains(TAG_TEMPORARY)) {
            long now = mob.getWorld().getTime();
            boolean isProtected = false;

            for (String tag : mob.getCommandTags()) {
                if (tag.startsWith(TAG_BURN_PREFIX)) {
                    try {
                        long burnTime = Long.parseLong(tag.substring(TAG_BURN_PREFIX.length()));
                        if (now < burnTime) isProtected = true;
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }

            if (isProtected) {
                if (mob.isOnFire()) mob.extinguish();
            } else {
                mob.removeCommandTag(TAG_TEMPORARY);
            }
        }
    }

    private static void applyErodedStats(HostileEntity mob, int mined, float threat, TerritoryConfig.Server cfg) {
        double baseHp;

        if (mined <= cfg.titleMidThreshold) {
            baseHp = 2.0;
        } else if (mined <= cfg.titleHighThreshold) {
            double t = (double) (mined - cfg.titleMidThreshold) / (cfg.titleHighThreshold - cfg.titleMidThreshold);
            baseHp = 4.0 + (t * 16.0);
        } else {
            double t = Math.min(1.0, (double) (mined - cfg.titleHighThreshold) / 2000.0);
            baseHp = 22.0 + (t * 18.0);
        }

        double finalMaxHp = Math.min(cfg.mobMaxHp, baseHp + (threat * 20.0));

        EntityAttributeInstance hpAttr = mob.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.setBaseValue(finalMaxHp);
            mob.setHealth((float) finalMaxHp);
        }

        TitleInfo info = getTitleByMined(mined, cfg);
        String titleKey = switch(info.name.toLowerCase()) {
            case "forsaken" -> "eroded.mob.title.forsaken";
            case "eroded" -> "eroded.mob.title.eroded";
            case "apocalypse" -> "eroded.mob.title.apocalypse";
            default -> "eroded.mob.title.forsaken";
        };

        mob.setCustomName(
                Text.empty()
                        .append(mob.getType().getName())
                        .append(" ")
                        .append(Text.translatable(titleKey))
                        .formatted(info.color)
        );
        mob.setCustomNameVisible(cfg.MobNameVisible);
    }

    private static BlockPos findSpawnPos(ServerWorld world, BlockPos center, Random random, TerritoryConfig.Server cfg) {
        int r = (int) cfg.spawnMaxDistance;
        double minSq = cfg.spawnMinDistance * cfg.spawnMinDistance;

        for (int i = 0; i < cfg.spawnAttempts; i++) {
            int x = center.getX() + random.nextBetween(-r, r);
            int z = center.getZ() + random.nextBetween(-r, r);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

            if (cfg.surfaceOnlySpawns) {
                int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
                if (y < surfaceY - cfg.undergroundTolerance) continue;
            }

            BlockPos pos = new BlockPos(x, y, z);
            if (center.getSquaredDistance(pos.toCenterPos()) < minSq) continue;

            if (world.getBlockState(pos.down()).isSolidBlock(world, pos.down()) && world.isAir(pos) && world.isAir(pos.up())) {
                return pos;
            }
        }
        return null;
    }

    private record TitleInfo(String name, Formatting color) {}

    private static TitleInfo getTitleByMined(int mined, TerritoryConfig.Server cfg) {
        if (mined <= cfg.titleMidThreshold) return new TitleInfo(cfg.titleLow, Formatting.GRAY);
        if (mined <= cfg.titleHighThreshold) return new TitleInfo(cfg.titleMid, Formatting.YELLOW);
        return new TitleInfo(cfg.titleHigh, Formatting.RED);
    }
}
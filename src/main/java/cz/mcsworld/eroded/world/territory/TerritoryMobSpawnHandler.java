package cz.mcsworld.eroded.world.territory;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.core.ErodedEntities;
import cz.mcsworld.eroded.entity.ErodedMobDespawnBehaviour;
import cz.mcsworld.eroded.entity.ErodedMobSunBehaviour;
import cz.mcsworld.eroded.world.darkness.MutatedMobResolver;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TerritoryMobSpawnHandler {

    private static final String TAG_ERODED = "eroded_special_mob";
    private static final String TAG_PERMANENT = "eroded_sun_proof";
    private static final String TAG_TEMPORARY = "eroded_temp_proof";
    private static final String TAG_BURN_PREFIX = "burn_at_";

    private TerritoryMobSpawnHandler() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(TerritoryMobSpawnHandler::onWorldTick);
    }

    private static void onWorldTick(ServerLevel world) {
        var cfg = TerritoryConfig.get().server;

        if (!cfg.enabled || !cfg.mobSpawnControlEnabled) {
            return;
        }

        if (world.getGameTime() % cfg.spawnCheckInterval == 0) {
            for (ServerPlayer player : world.players()) {
                trySpawnErodedMob(player, world, cfg);
            }
        }

        // Sun/protection upkeep does not need a 20 Hz radius scan. Run it
        // once per second and deduplicate mobs seen by overlapping players.
        if (world.getGameTime() % 20L == 0L) {
            handleMobBehaviour(world, cfg);
        }
    }

    private static void trySpawnErodedMob(
            ServerPlayer player,
            ServerLevel world,
            TerritoryConfig.Server cfg
    ) {
        BlockPos pPos = player.blockPosition();

        if (cfg.spawnProtectionEnabled) {
            if (pPos.closerThan(world.getSharedSpawnPos(), cfg.spawnProtectionRadius)) {
                return;
            }
        }

        ChunkPos cp = new ChunkPos(pPos);
        TerritoryWorldState worldState = TerritoryWorldState.getIfPresent(world);
        if (worldState == null) {
            return;
        }
        TerritoryCell cell = worldState.getCell(TerritoryCellKey.fromChunk(cp.x, cp.z));
        if (cell == null) {
            return;
        }

        int mined = cell.getMiningScore();

        if (mined < cfg.miningThreshold) {
            return;
        }

        RandomSource random = world.getRandom();
        float threat = TerritoryThreatResolver.computeThreat(cell, world.getGameTime());

        float spawnChance = Math.max(cfg.spawnKeepMinChance, threat * 0.4f);

        if (random.nextFloat() > spawnChance) {
            return;
        }

        if (cfg.maxMobsPerSpawnCycle <= 0) {
            return;
        }

        // A successful spawn cycle must spawn at least one mob.
        // maxMobsPerSpawnCycle=1 therefore means exactly one mob, not 0-1.
        int count = 1 + random.nextInt(cfg.maxMobsPerSpawnCycle);

        for (int i = 0; i < count; i++) {
            BlockPos spawnPos = findSpawnPos(world, pPos, random, cfg);

            if (spawnPos == null) {
                continue;
            }

            ChunkPos spawnChunk = new ChunkPos(spawnPos);
            if (countErodedMobsInChunk(world, spawnChunk) >= cfg.mobMaxPerChunk) {
                continue;
            }

            EntityType<? extends Monster> type = random.nextBoolean()
                    ? ErodedEntities.ERODED_SPECIAL_ZOMBIE
                    : ErodedEntities.ERODED_SPECIAL_SKELETON;

            Monster mob = type.create(world, EntitySpawnReason.EVENT);

            if (mob == null) {
                continue;
            }

            mob.snapTo(
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    random.nextFloat() * 360.0F,
                    0.0F
            );

            mob.finalizeSpawn(
                    world,
                    world.getCurrentDifficultyAt(spawnPos),
                    EntitySpawnReason.EVENT,
                    null
            );

            mob.addTag(TAG_ERODED);
            mob.addTag(ErodedMobDespawnBehaviour.TAG_TERRITORY_SPAWN);

            if (cfg.mobBuffEnabled && threat > cfg.mobBuffThreshold) {
                mob.addTag(MutatedMobResolver.MUTATED_TAG);
            }

            applyErodedStats(mob, mined, threat, cfg);
            applySpawnBehaviour(mob, random);

            if (mob instanceof AbstractSkeleton skeleton) {
                if (skeleton.getMainHandItem().isEmpty()) {
                    skeleton.setItemSlot(
                            EquipmentSlot.MAINHAND,
                            new ItemStack(Items.BOW)
                    );
                }

                skeleton.reassessWeaponGoal();
            }

            world.addFreshEntityWithPassengers(mob);
        }
    }

    private static int countErodedMobsInChunk(ServerLevel world, ChunkPos chunkPos) {
        AABB chunkBounds = new AABB(
                chunkPos.getMinBlockX(),
                world.getMinY(),
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1,
                world.getMaxY() + 1,
                chunkPos.getMaxBlockZ() + 1
        );

        return world.getEntitiesOfClass(
                Monster.class,
                chunkBounds,
                mob -> mob.getTags().contains(TAG_ERODED)
        ).size();
    }

    private static void applySpawnBehaviour(
            Monster mob,
            RandomSource random
    ) {
        if (mob.getType() == ErodedEntities.ERODED_SPECIAL_SKELETON
                || mob.getType() == ErodedEntities.ERODED_SPECIAL_ZOMBIE) {
            ErodedMobSunBehaviour.applyRandomSunBehaviour(mob, random);
        }
    }

    private static void handleMobBehaviour(ServerLevel world, TerritoryConfig.Server cfg) {
        Set<UUID> processed = new HashSet<>();

        for (ServerPlayer player : world.players()) {
            AABB box = new AABB(player.blockPosition()).inflate(cfg.mobDespawnRadius);

            List<Monster> nearby = world.getEntitiesOfClass(
                    Monster.class,
                    box,
                    e -> e.getTags().contains(TAG_ERODED)
            );

            for (Monster mob : nearby) {
                if (processed.add(mob.getUUID())) {
                    processProtection(mob, world);
                }
            }
        }
    }

    private static void processProtection(Monster mob, ServerLevel world) {
        if (!mob.isAlive()) {
            return;
        }

        if (mob.getType() == ErodedEntities.ERODED_SPECIAL_SKELETON
                || mob.getType() == ErodedEntities.ERODED_SPECIAL_ZOMBIE) {
            return;
        }

        if (mob.getTags().contains(TAG_PERMANENT)) {
            if (mob.isOnFire()) {
                mob.clearFire();
            }

            return;
        }

        if (mob.getTags().contains(TAG_TEMPORARY)) {
            long burnTime = getBurnTime(mob);
            long now = world.getGameTime();

            if (burnTime < 0L || now < burnTime) {
                if (mob.isOnFire()) {
                    mob.clearFire();
                }

                return;
            }

            mob.removeTag(TAG_TEMPORARY);
            removeBurnTimeTag(mob);
        }

        if (mob.getType() == ErodedEntities.ERODED_SPECIAL_SKELETON) {
            if (isInDirectDaylight(mob, world)) {
                mob.igniteForSeconds(8.0F);
            }
        }
    }

    private static long getBurnTime(Monster mob) {
        for (String tag : mob.getTags()) {
            if (!tag.startsWith(TAG_BURN_PREFIX)) {
                continue;
            }

            try {
                return Long.parseLong(tag.substring(TAG_BURN_PREFIX.length()));
            } catch (NumberFormatException ignored) {
                return -1L;
            }
        }

        return -1L;
    }

    private static void removeBurnTimeTag(Monster mob) {
        String burnTag = null;

        for (String tag : mob.getTags()) {
            if (tag.startsWith(TAG_BURN_PREFIX)) {
                burnTag = tag;
                break;
            }
        }

        if (burnTag != null) {
            mob.removeTag(burnTag);
        }
    }

    private static boolean isInDirectDaylight(Monster mob, ServerLevel world) {
        return world.isBrightOutside()
                && world.canSeeSky(mob.blockPosition())
                && !mob.isInWaterOrRain();
    }

    private static void applyErodedStats(
            Monster mob,
            int mined,
            float threat,
            TerritoryConfig.Server cfg
    ) {
        double baseHp;

        if (mined <= cfg.titleMidThreshold) {
            baseHp = 2.0;
        } else if (mined <= cfg.titleHighThreshold) {
            double t = (double) (mined - cfg.titleMidThreshold)
                    / (cfg.titleHighThreshold - cfg.titleMidThreshold);

            baseHp = 4.0 + (t * 16.0);
        } else {
            double t = Math.min(
                    1.0,
                    (double) (mined - cfg.titleHighThreshold) / 2000.0
            );

            baseHp = 22.0 + (t * 18.0);
        }

        double finalMaxHp = Math.min(
                cfg.mobMaxHp,
                baseHp + (threat * 20.0)
        );

        AttributeInstance hpAttr = mob.getAttribute(Attributes.MAX_HEALTH);

        if (hpAttr != null) {
            hpAttr.setBaseValue(finalMaxHp);
            mob.setHealth((float) finalMaxHp);
        }

        TitleInfo info = getTitleByMined(mined, cfg);

        String titleKey = switch (info.name.toLowerCase()) {
            case "eroded" -> "eroded.mob.title.eroded";
            case "apocalypse" -> "eroded.mob.title.apocalypse";
            default -> "eroded.mob.title.forsaken";
        };

        Component mobName = mob.getType() == ErodedEntities.ERODED_SPECIAL_SKELETON
                ? Component.translatable("entity.minecraft.skeleton")
                : mob.getType().getDescription();

        mob.setCustomName(
                Component.empty()
                        .append(Component.translatable(titleKey))
                        .append(" ")
                        .append(mobName)
                        .withStyle(info.color)
        );

        mob.setCustomNameVisible(cfg.MobNameVisible);

    }

    private static BlockPos findSpawnPos(
            ServerLevel world,
            BlockPos center,
            RandomSource random,
            TerritoryConfig.Server cfg
    ) {
        int r = Math.max(1, (int) Math.ceil(cfg.spawnMaxDistance));
        double minSq = cfg.spawnMinDistance * cfg.spawnMinDistance;

        for (int i = 0; i < cfg.spawnAttempts; i++) {
            int x = center.getX() + random.nextIntBetweenInclusive(-r, r);
            int z = center.getZ() + random.nextIntBetweenInclusive(-r, r);

            BlockPos columnProbe = new BlockPos(x, center.getY(), z);
            if (!world.hasChunkAt(columnProbe)) {
                continue;
            }

            BlockPos pos;

            if (cfg.surfaceOnlySpawns) {
                int y = world.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z
                );

                int surfaceY = world.getHeight(
                        Heightmap.Types.WORLD_SURFACE,
                        x,
                        z
                );

                if (y < surfaceY - cfg.undergroundTolerance) {
                    continue;
                }

                pos = new BlockPos(x, y, z);

                if (!isValidSpawnPos(world, pos)) {
                    continue;
                }
            } else {
                // Do not use a surface heightmap here. Search around the player's
                // current Y level so a player mining in a cave can actually get
                // cave spawns. The nearest usable floor is preferred.
                pos = findLocalSpawnPos(world, x, z, center.getY(), r, random);
                if (pos == null) {
                    continue;
                }
            }

            if (center.distToCenterSqr(pos.getCenter()) < minSq) {
                continue;
            }

            return pos;
        }

        return null;
    }

    private static BlockPos findLocalSpawnPos(
            ServerLevel world,
            int x,
            int z,
            int centerY,
            int verticalRange,
            RandomSource random
    ) {
        int minY = world.getMinY() + 1;
        int maxY = world.getMaxY() - 2;
        boolean upFirst = random.nextBoolean();

        for (int offset = 0; offset <= verticalRange; offset++) {
            if (offset == 0) {
                int y = Math.max(minY, Math.min(maxY, centerY));
                BlockPos pos = new BlockPos(x, y, z);
                if (isValidSpawnPos(world, pos)) {
                    return pos;
                }
                continue;
            }

            int firstY = centerY + (upFirst ? offset : -offset);
            if (firstY >= minY && firstY <= maxY) {
                BlockPos pos = new BlockPos(x, firstY, z);
                if (isValidSpawnPos(world, pos)) {
                    return pos;
                }
            }

            int secondY = centerY + (upFirst ? -offset : offset);
            if (secondY >= minY && secondY <= maxY) {
                BlockPos pos = new BlockPos(x, secondY, z);
                if (isValidSpawnPos(world, pos)) {
                    return pos;
                }
            }
        }

        return null;
    }

    private static boolean isValidSpawnPos(ServerLevel world, BlockPos pos) {
        return world.getBlockState(pos.below()).isRedstoneConductor(world, pos.below())
                && world.isEmptyBlock(pos)
                && world.isEmptyBlock(pos.above());
    }

    private record TitleInfo(String name, ChatFormatting color) {
    }

    private static TitleInfo getTitleByMined(int mined, TerritoryConfig.Server cfg) {
        if (mined <= cfg.titleMidThreshold) {
            return new TitleInfo(cfg.titleLow, ChatFormatting.GRAY);
        }

        if (mined <= cfg.titleHighThreshold) {
            return new TitleInfo(cfg.titleMid, ChatFormatting.YELLOW);
        }

        return new TitleInfo(cfg.titleHigh, ChatFormatting.RED);
    }
}
package cz.mcsworld.eroded.world.territory.ecosystem;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.territory.TerritoryCell;
import cz.mcsworld.eroded.world.territory.TerritoryCellKey;
import cz.mcsworld.eroded.world.territory.TerritoryThreatResolver;
import cz.mcsworld.eroded.world.territory.TerritoryWorldState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.List;

public final class TerritoryEcosystemTicker {

    private static final int FALLBACK_INTERVAL_TICKS = 20;
    private static int rrIndex = 0;
    private static int tickCounter = 0;

    private TerritoryEcosystemTicker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TerritoryEcosystemTicker::onTick);
    }

    private static void onTick(MinecraftServer server) {
        var root = TerritoryConfig.get();
        var cfg = root.server;
        if (!cfg.enabled || !cfg.ecosystemEnabled) return;

        int interval = cfg.ecosystemIntervalTicks > 0 ? cfg.ecosystemIntervalTicks : FALLBACK_INTERVAL_TICKS;

        if (++tickCounter < interval) return;
        tickCounter = 0;

        for (ServerLevel world : server.getAllLevels()) {
            tickWorld(world);
        }
    }

    private static void tickWorld(ServerLevel world) {
        var cfg = TerritoryConfig.get().server;

        List<ServerPlayer> players = world.players();
        if (players.isEmpty()) return;

        long tick = world.getGameTime();
        RandomSource random = world.getRandom();
        TerritoryWorldState state = TerritoryWorldState.getIfPresent(world);
        if (state == null) return;

        int radius = Math.max(8, cfg.ecosystemVisibleRadiusBlocks);
        int maxPlayers = Math.max(1, cfg.ecosystemMaxPlayersPerSlice);

        int attemptsPerPlayer = Math.max(0, cfg.ecosystemAttemptsPerPlayer);
        int surfaceAttempts = Math.max(0, cfg.ecosystemSurfaceAttempts);
        int leafAttempts = Math.max(0, cfg.ecosystemLeafAttempts);

        if (attemptsPerPlayer <= 0 && surfaceAttempts <= 0 && leafAttempts <= 0) return;

        if (surfaceAttempts + leafAttempts <= 0) {
            surfaceAttempts = attemptsPerPlayer;
            leafAttempts = 0;
        }

        int leafMinY = Math.min(cfg.ecosystemLeafMinY, cfg.ecosystemLeafMaxY);
        int leafMaxY = Math.max(cfg.ecosystemLeafMinY, cfg.ecosystemLeafMaxY);

        float degradeThr = cfg.ecosystemDegradeThreatThreshold;
        float regenThr = cfg.ecosystemRegenThreatThreshold;

        int count = Math.min(maxPlayers, players.size());

        for (int i = 0; i < count; i++) {
            int idx = rrIndex++ % players.size();
            ServerPlayer player = players.get(idx);
            ChunkPos cp = new ChunkPos(player.blockPosition());
            TerritoryCellKey key = TerritoryCellKey.fromChunk(cp.x, cp.z);
            TerritoryCell cell = state.getCell(key);
            if (cell == null) continue;

            float threat = TerritoryThreatResolver.computeThreat(cell, tick);
            int pollution = cell.getPollution(tick);
            int miningScore = cell.getMiningScore();

            long lastMiningTick = cell.getLastMiningActivityTick();
            long ticksSinceMining = lastMiningTick == 0 ? Integer.MAX_VALUE : tick - lastMiningTick;

            int calmDownDelay = cfg.ecosystemCalmDownDelay;
            boolean recentlyMining = ticksSinceMining < calmDownDelay;

            if (threat < 0.10f && pollution < 10 && miningScore <= 0) {
                continue;
            }

            boolean doDegrade = recentlyMining && threat >= degradeThr;

            boolean doRegen = !recentlyMining
                    && threat <= regenThr
                    && (pollution > 0 || miningScore > 0);

            if (!doDegrade && !doRegen) continue;

            BlockPos center = player.blockPosition();

            if (doDegrade) {
                for (int a = 0; a < surfaceAttempts; a++) {
                    maybeDegradeSurfaceNearPlayer(world, center, radius, random);
                }
                for (int a = 0; a < leafAttempts; a++) {
                    maybeWitherLeavesNearPlayer(world, center, radius, leafMinY, leafMaxY, random, cfg);
                }
            } else {
                cell.addForestation(1, tick);
                cell.addPollution(-1, tick);
                cell.addMining(-1, tick);

                for (int a = 0; a < surfaceAttempts; a++) {
                    maybeRegrowNearPlayer(world, center, radius, random);
                }
            }
        }
    }

    private static void maybeDegradeSurfaceNearPlayer(ServerLevel world, BlockPos center, int radius, RandomSource random) {

        var cfg = TerritoryConfig.get().server;
        if (random.nextFloat() > cfg.grassDegradeChance) return;

        BlockPos groundPos = randomSurfaceNearPlayer(world, center, radius, random);
        if (groundPos == null) return;

        BlockState old = world.getBlockState(groundPos);
        BlockState newState = null;

        if (old.is(Blocks.GRASS_BLOCK)) {
            newState = Blocks.DIRT.defaultBlockState();
        } else if (old.is(Blocks.DIRT)) {
            newState = Blocks.COARSE_DIRT.defaultBlockState();
        } else if (old.is(Blocks.COARSE_DIRT) && random.nextFloat() < 0.20f) {
            newState = Blocks.PODZOL.defaultBlockState();
        } else if (old.is(Blocks.MOSS_BLOCK) && random.nextFloat() < 0.60f) {
            newState = Blocks.DIRT.defaultBlockState();
        } else if (old.is(Blocks.PODZOL) && random.nextFloat() < 0.10f) {
            newState = Blocks.DIRT.defaultBlockState();
        }

        if (newState != null && newState != old) {
            world.setBlock(groundPos, newState, 2);
        }

        if (random.nextFloat() < 0.70f) {
            BlockPos above = groundPos.above();
            BlockState a = world.getBlockState(above);

            if (a.isAir()) return;

            world.destroyBlock(above, false);
        }
    }

    private static void maybeRegrowNearPlayer(ServerLevel world, BlockPos center, int radius, RandomSource random) {

        var cfg = TerritoryConfig.get().server;
        if (random.nextFloat() > cfg.grassRegrowChance) return;

        BlockPos pos = randomSurfaceNearPlayer(world, center, radius, random);
        if (pos == null) return;

        if (world.getMaxLocalRawBrightness(pos.above()) < 9) return;

        BlockState old = world.getBlockState(pos);

        if (old.is(Blocks.DIRT) || old.is(Blocks.COARSE_DIRT)) {
            world.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        }
    }

    private static void maybeWitherLeavesNearPlayer(
            ServerLevel world,
            BlockPos center,
            int radius,
            int leafMinY,
            int leafMaxY,
            RandomSource random,
            TerritoryConfig.Server cfg
    ) {
        float leafAttemptChance = cfg.permanentScarChance * cfg.ecosystemLeafLossMultiplier;
        leafAttemptChance = Math.max(cfg.ecosystemLeafLossMinChance, leafAttemptChance);
        leafAttemptChance = Math.min(cfg.ecosystemLeafLossMaxChance, leafAttemptChance);

        if (random.nextFloat() > leafAttemptChance) {
            return;
        }

        int minY = Math.max(world.getMinY(), Math.min(leafMinY, leafMaxY));

        int worldTopY = world.getMinY() + world.getHeight() - 1;
        int maxY = Math.min(worldTopY, Math.max(leafMinY, leafMaxY));

        if (maxY <= minY) {
            return;
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
            int y = minY + random.nextInt(maxY - minY + 1);

            BlockPos checkPos = new BlockPos(x, y, z);
            if (!world.hasChunkAt(checkPos)) {
                continue;
            }
            BlockState state = world.getBlockState(checkPos);

            if (state.is(BlockTags.LEAVES)) {
                world.destroyBlock(checkPos, false);
                return;
            }
        }
    }

    private static BlockPos randomSurfaceNearPlayer(ServerLevel world, BlockPos center, int radius, RandomSource random) {
        int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
        int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;

        BlockPos probe = new BlockPos(x, center.getY(), z);
        if (!world.hasChunkAt(probe)) {
            return null;
        }

        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        if (y < world.getMinY()) return null;

        return new BlockPos(x, y, z);
    }
}
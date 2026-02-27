package cz.mcsworld.eroded.world.territory.ecosystem;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.territory.TerritoryCell;
import cz.mcsworld.eroded.world.territory.TerritoryCellKey;
import cz.mcsworld.eroded.world.territory.TerritoryThreatResolver;
import cz.mcsworld.eroded.world.territory.TerritoryWorldState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

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

        for (ServerWorld world : server.getWorlds()) {
            tickWorld(world);
        }
    }

    private static void tickWorld(ServerWorld world) {
        var cfg = TerritoryConfig.get().server;

        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;

        long tick = world.getServer().getTicks();
        Random random = world.getRandom();
        TerritoryWorldState state = TerritoryWorldState.get(world);

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

        int count = Math.min(maxPlayers, players.size());

        for (int i = 0; i < count; i++) {
            int idx = rrIndex++ % players.size();
            ServerPlayerEntity player = players.get(idx);

            ChunkPos cp = new ChunkPos(player.getBlockPos());
            TerritoryCellKey key = TerritoryCellKey.fromChunk(cp.x, cp.z);
            TerritoryCell cell = state.getOrCreateCell(key);

            float threat = TerritoryThreatResolver.computeThreat(cell, tick);
            int pollution = cell.getPollution(tick);
            int miningScore = cell.getMiningScore();
            long lastMiningTick = cell.getLastMiningActivityTick();
            long ticksSinceMining = lastMiningTick == 0 ? Integer.MAX_VALUE : tick - lastMiningTick;

            int calmDownDelay = cfg.ecosystemCalmDownDelay;
            boolean recentlyMining = ticksSinceMining < calmDownDelay;

            if (threat < 0.10f && pollution < 10) continue;

            boolean doDegrade = threat > degradeThr && recentlyMining;
            boolean doRegen = !recentlyMining;

            if (!doDegrade && !doRegen) continue;

            BlockPos center = player.getBlockPos();

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

    private static void maybeDegradeSurfaceNearPlayer(ServerWorld world, BlockPos center, int radius, Random random) {

        var cfg = TerritoryConfig.get().server;
        if (random.nextFloat() > cfg.grassDegradeChance) return;

        BlockPos groundPos = randomSurfaceNearPlayer(world, center, radius, random);
        if (groundPos == null) return;

        BlockState old = world.getBlockState(groundPos);
        BlockState newState = null;

        if (old.isOf(Blocks.GRASS_BLOCK)) {
            newState = Blocks.DIRT.getDefaultState();
        } else if (old.isOf(Blocks.DIRT)) {
            newState = Blocks.COARSE_DIRT.getDefaultState();
        } else if (old.isOf(Blocks.COARSE_DIRT) && random.nextFloat() < 0.20f) {
            newState = Blocks.PODZOL.getDefaultState();
        } else if (old.isOf(Blocks.MOSS_BLOCK) && random.nextFloat() < 0.60f) {
            newState = Blocks.DIRT.getDefaultState();
        } else if (old.isOf(Blocks.PODZOL) && random.nextFloat() < 0.10f) {
            newState = Blocks.DIRT.getDefaultState();
        }

        if (newState != null && newState != old) {
            world.setBlockState(groundPos, newState, 2);
        }

        if (random.nextFloat() < 0.70f) {
            BlockPos above = groundPos.up();
            BlockState a = world.getBlockState(above);

            if (a.isAir()) return;

            world.breakBlock(above, false);
        }
    }

    private static void maybeRegrowNearPlayer(ServerWorld world, BlockPos center, int radius, Random random) {

        var cfg = TerritoryConfig.get().server;
        if (random.nextFloat() > cfg.grassRegrowChance) return;

        BlockPos pos = randomSurfaceNearPlayer(world, center, radius, random);
        if (pos == null) return;

        if (world.getLightLevel(pos.up()) < 9) return;

        BlockState old = world.getBlockState(pos);

        if (old.isOf(Blocks.DIRT) || old.isOf(Blocks.COARSE_DIRT)) {
            world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState(), 2);
        }
    }

    private static void maybeWitherLeavesNearPlayer(
            ServerWorld world,
            BlockPos center,
            int radius,
            int leafMinY,
            int leafMaxY,
            Random random,
            TerritoryConfig.Server cfg
    ) {

        float leafAttemptChance = cfg.permanentScarChance * cfg.ecosystemLeafLossMultiplier;
        leafAttemptChance = Math.max(cfg.ecosystemLeafLossMinChance, leafAttemptChance);
        leafAttemptChance = Math.min(cfg.ecosystemLeafLossMaxChance, leafAttemptChance);

        if (random.nextFloat() > leafAttemptChance) return;

        BlockPos base = randomSurfaceNearPlayer(world, center, radius, random);
        if (base == null) return;

        for (int y = 2; y <= 20; y++) {
            BlockPos checkPos = base.up(y);
            BlockState state = world.getBlockState(checkPos);

            if (state.isAir()) continue;

            world.breakBlock(checkPos, false);
            return;
        }
    }

    private static BlockPos randomSurfaceNearPlayer(ServerWorld world, BlockPos center, int radius, Random random) {
        int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
        int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;

        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        if (y < world.getBottomY()) return null;

        return new BlockPos(x, y, z);
    }
}
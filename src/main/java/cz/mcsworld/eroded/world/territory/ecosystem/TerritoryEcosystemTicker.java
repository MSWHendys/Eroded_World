package cz.mcsworld.eroded.world.territory.ecosystem;

import cz.mcsworld.eroded.config.debug.DebugConfig;
import cz.mcsworld.eroded.world.territory.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

public final class TerritoryEcosystemTicker {

    private static final int INTERVAL_TICKS = 100;

    private static final int POLLUTION_REGEN_BLOCK = 50;
    private static final int POLLUTION_SCAR = 80;

    private static int counter = 0;

    private TerritoryEcosystemTicker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TerritoryEcosystemTicker::onTick);
    }

    private static void onTick(MinecraftServer server) {

        if (++counter < INTERVAL_TICKS) return;
        counter = 0;

        for (ServerWorld world : server.getWorlds()) {
            tickWorld(world);
        }
    }

    private static void tickWorld(ServerWorld world) {
        long tick = world.getServer().getTicks();
        Random random = world.getRandom();

        TerritoryWorldState state = TerritoryWorldState.get(world);

        for (ServerPlayerEntity player : world.getPlayers()) {
            ChunkPos cp = new ChunkPos(player.getBlockPos());
            TerritoryCellKey key = TerritoryCellKey.fromChunk(cp.x, cp.z);

            TerritoryCell cell = state.getOrCreateCell(key);
            TerritoryData data = TerritoryStorage.get(world, cp);

            float threat = TerritoryThreatResolver.computeThreat(data, tick);
            int pollution = data.getPollution(tick);

            maybeErodeStone(world, key, cell, threat, random);

            if (threat > 0.6f) {
                maybeDegradeGrass(world, key, random);
            }

            if (threat < 0.25f && pollution < POLLUTION_REGEN_BLOCK) {
                maybeRegrowGrass(world, key, random);
            }

            if (threat > 0.85f && pollution > POLLUTION_SCAR) {
                maybeApplyPermanentScar(world, key, random);
            }

            maybePlayAtmosphere(world, key, threat, pollution, random);
        }
    }

    private static void maybeErodeStone(
            ServerWorld world,
            TerritoryCellKey key,
            TerritoryCell cell,
            float threat,
            Random random
    ) {
        if (threat < 0.6f) return;
        if (cell.getMiningScore() < 300) return;
        if (random.nextFloat() >  0.25f) return;

        ChunkPos cp = randomChunkFromCell(key, random);
        if (!world.isChunkLoaded(cp.x, cp.z)) return;

        BlockPos pos = randomUndergroundPos(cp, random);
        if (world.getBlockState(pos).isOf(Blocks.STONE)) {
            world.setBlockState(pos, Blocks.GRAVEL.getDefaultState(), 2);
        }

    }

    private static void maybeDegradeGrass(
            ServerWorld world,
            TerritoryCellKey key,
            Random random
    ) {
        if (random.nextFloat() > 0.25f) return;

        ChunkPos cp = randomChunkFromCell(key, random);
        if (!world.isChunkLoaded(cp.x, cp.z)) return;

        BlockPos groundPos = randomSurfacePos(world, cp, random);
        if (groundPos == null) return;

        BlockPos abovePos = groundPos.up();

        var groundState = world.getBlockState(groundPos);
        var aboveState  = world.getBlockState(abovePos);

        boolean changed = false;

        if (groundState.isOf(Blocks.GRASS_BLOCK)) {
            world.setBlockState(
                    groundPos,
                    Blocks.COARSE_DIRT.getDefaultState(),
                    2
            );
            changed = true;
        }

        if (aboveState.isOf(Blocks.TALL_GRASS)
                || aboveState.isOf(Blocks.SHORT_GRASS)
                || aboveState.isOf(Blocks.FERN)
                || aboveState.isOf(Blocks.LARGE_FERN)) {

            world.breakBlock(abovePos, false);
            changed = true;
        }

        if (changed && DebugConfig.get().ecosystem.enabled) {
            world.getServer().getPlayerManager().broadcast(
                    Text.translatable(
                            "eroded.ecosystem.surface_degraded",
                            cp.x,
                            cp.z
                    ),
                    false
            );
        }

    }

    private static void maybeRegrowGrass(
            ServerWorld world,
            TerritoryCellKey key,
            Random random
    ) {
        if (random.nextFloat() > 0.01f) return;

        ChunkPos cp = randomChunkFromCell(key, random);
        if (!world.isChunkLoaded(cp.x, cp.z)) return;

        BlockPos pos = randomSurfacePos(world, cp, random);
        if (pos == null) return;
        if (world.getLightLevel(pos.up()) < 9) return;

        var state = world.getBlockState(pos);

        if (state.isOf(Blocks.DIRT) || state.isOf(Blocks.COARSE_DIRT)) {
            world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState(), 2);
        } else if (state.isOf(Blocks.STONE) && random.nextFloat() < 0.2f) {
            world.setBlockState(pos, Blocks.MOSS_BLOCK.getDefaultState(), 2);
        }
    }

    private static void maybeApplyPermanentScar(
            ServerWorld world,
            TerritoryCellKey key,
            Random random
    ) {
        if (random.nextFloat() > 0.01f) return;

        ChunkPos cp = randomChunkFromCell(key, random);
        if (!world.isChunkLoaded(cp.x, cp.z)) return;

        BlockPos pos = randomSurfacePos(world, cp, random);
        if (pos == null) return;

        var state = world.getBlockState(pos);

        if (state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.COARSE_DIRT)) {

            world.setBlockState(pos, Blocks.PODZOL.getDefaultState(), 2);
        }

        if (state.isOf(Blocks.GRAVEL)) {
            world.setBlockState(pos, Blocks.TUFF.getDefaultState(), 2);
        }

        if (state.isOf(Blocks.STONE)) {
            world.setBlockState(pos, Blocks.ANDESITE.getDefaultState(), 2);
        }
    }

    private static void maybePlayAtmosphere(
            ServerWorld world,
            TerritoryCellKey key,
            float threat,
            int pollution,
            Random random
    ) {
        if (threat < 0.7f && pollution < 60) return;
        if (random.nextFloat() > 0.05f) return;

        ChunkPos cp = randomChunkFromCell(key, random);
        BlockPos pos = randomSurfacePos(world, cp, random);
        if (pos == null) return;

        if (world.getClosestPlayer(
                pos.getX(), pos.getY(), pos.getZ(),
                32,
                false
        ) == null) return;

        world.spawnParticles(
                ParticleTypes.ASH,
                pos.getX() + 0.5,
                pos.getY() + 1.2,
                pos.getZ() + 0.5,
                6,
                0.3, 0.2, 0.3,
                0.01
        );

        if (random.nextFloat() < 0.3f) {
            world.playSound(
                    null,
                    pos,
                    SoundEvents.BLOCK_CAMPFIRE_CRACKLE,
                    SoundCategory.AMBIENT,
                    0.3f,
                    0.7f
            );
        }
    }

    private static ChunkPos randomChunkFromCell(
            TerritoryCellKey key,
            Random random
    ) {
        int baseX = key.cellX() * TerritoryCellKey.CELL_SIZE;
        int baseZ = key.cellZ() * TerritoryCellKey.CELL_SIZE;

        return new ChunkPos(
                baseX + random.nextInt(TerritoryCellKey.CELL_SIZE),
                baseZ + random.nextInt(TerritoryCellKey.CELL_SIZE)
        );
    }

    private static BlockPos randomSurfacePos(
            ServerWorld world,
            ChunkPos cp,
            Random random
    ) {
        int x = cp.getStartX() + random.nextInt(16);
        int z = cp.getStartZ() + random.nextInt(16);

        int y = world.getTopY(
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                x,
                z
        ) - 1;

        if (y < world.getBottomY()) return null;
        return new BlockPos(x, y, z);
    }

    private static BlockPos randomUndergroundPos(
            ChunkPos cp,
            Random random
    ) {
        int x = cp.getStartX() + random.nextInt(16);
        int y = random.nextInt(40);
        int z = cp.getStartZ() + random.nextInt(16);
        return new BlockPos(x, y, z);
    }
    public static void forceTick(ServerWorld world) {
        tickWorld(world);
    }

}

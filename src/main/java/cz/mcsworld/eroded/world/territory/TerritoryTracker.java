package cz.mcsworld.eroded.world.territory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class TerritoryTracker {

    private TerritoryTracker() {}

    public static void onBlockPlaced(ServerWorld world, BlockPos pos, BlockState blockState) {
        long tick = world.getServer().getTicks();
        Block block = blockState.getBlock();

        int forest = resolveForestationValue(blockState);
        int pollution = resolvePollutionValue(blockState);

        if (forest > 0 || pollution > 0) {
            updateCell(world, pos, tick, cell -> {
                if (forest > 0) cell.addForestation(forest, tick);
                if (pollution > 0) cell.addPollution(pollution, tick);
            });
        }
    }

    public static void onBlockBroken(ServerWorld world, BlockPos pos, BlockState blockState) {
        long tick = world.getServer().getTicks();
        int mining = resolveMiningValue(blockState, pos);

        if (mining > 0) {
            updateCell(world, pos, tick, cell -> cell.addMining(mining, tick));
        }
    }

    private static void updateCell(ServerWorld world, BlockPos pos, long tick, java.util.function.Consumer<TerritoryCell> action) {
        ChunkPos chunk = new ChunkPos(pos);
        TerritoryCellKey key = TerritoryCellKey.fromChunk(chunk.x, chunk.z);
        TerritoryWorldState worldState = TerritoryWorldState.get(world);
        TerritoryCell cell = worldState.getOrCreateCell(key);

        action.accept(cell);
        worldState.markDirty();
    }

    private static int resolveMiningValue(BlockState state, BlockPos pos) {

        if (state.isIn(BlockTags.GOLD_ORES) || state.isIn(BlockTags.IRON_ORES) ||
                state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.COAL_ORES) ||
                state.isIn(BlockTags.COPPER_ORES) || state.isIn(BlockTags.REDSTONE_ORES) ||
                state.isIn(BlockTags.LAPIS_ORES) || state.isIn(BlockTags.EMERALD_ORES)) {
            return 3;
        }

        if (state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isIn(BlockTags.BASE_STONE_NETHER)) {
            return pos.getY() < 0 ? 2 : 1;
        }

        return 0;
    }

    private static int resolveForestationValue(BlockState state) {
        if (state.isIn(BlockTags.LOGS)) {
            return 2;
        }

        if (state.isIn(BlockTags.SAPLINGS)) {
            return 1;
        }

        return 0;
    }

    private static int resolvePollutionValue(BlockState state) {

        if (state.isIn(BlockTags.CAMPFIRES) || state.isIn(BlockTags.FIRE)) {
            return 2;
        }

        Block block = state.getBlock();
        if (block == net.minecraft.block.Blocks.FURNACE ||
                block == net.minecraft.block.Blocks.BLAST_FURNACE ||
                block == net.minecraft.block.Blocks.SMOKER ||
                block == net.minecraft.block.Blocks.LAVA ||
                block == net.minecraft.block.Blocks.MAGMA_BLOCK) {
            return 2;
        }

        return 0;
    }
}
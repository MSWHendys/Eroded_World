package cz.mcsworld.eroded.world.territory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class TerritoryTracker {

    private TerritoryTracker() {}

    // ============================================================
    // BLOCK PLACE
    // ============================================================

    public static void onBlockPlaced(
            ServerWorld world,
            BlockPos pos,
            BlockState blockState
    ) {
        long tick = world.getServer().getTicks();

        ChunkPos chunk = new ChunkPos(pos);
        TerritoryCellKey key =
                TerritoryCellKey.fromChunk(chunk.x, chunk.z);

        TerritoryWorldState worldState =
                TerritoryWorldState.get(world);

        TerritoryCell cell =
                worldState.getOrCreateCell(key);

        Block block = blockState.getBlock();

        int forest = resolveForestationValue(block);
        int pollution = resolvePollutionValue(block);

        if (forest > 0) {
            cell.addForestation(forest, tick);
            worldState.markDirty();
        }

        if (pollution > 0) {
            cell.addPollution(pollution, tick);
            worldState.markDirty();
        }
    }

    // ============================================================
    // BLOCK BREAK
    // ============================================================

    public static void onBlockBroken(
            ServerWorld world,
            BlockPos pos,
            BlockState blockState
    ) {
        long tick = world.getServer().getTicks();

        ChunkPos chunk = new ChunkPos(pos);
        TerritoryCellKey key =
                TerritoryCellKey.fromChunk(chunk.x, chunk.z);

        TerritoryWorldState worldState =
                TerritoryWorldState.get(world);

        TerritoryCell cell =
                worldState.getOrCreateCell(key);

        int mining = resolveMiningValue(blockState, pos);
        if (mining > 0) {
            cell.addMining(mining, tick);
            worldState.markDirty();
        }
    }

    // ============================================================
    // VALUE RESOLVERS
    // ============================================================

    private static int resolveMiningValue(BlockState state, BlockPos pos) {
        Block block = state.getBlock();

        if (block == Blocks.STONE || block == Blocks.DEEPSLATE) {
            return pos.getY() < 0 ? 2 : 1;
        }

        if (block == Blocks.COAL_ORE
                || block == Blocks.IRON_ORE
                || block == Blocks.COPPER_ORE
                || block == Blocks.GOLD_ORE
                || block == Blocks.DIAMOND_ORE
                || block == Blocks.REDSTONE_ORE
                || block == Blocks.LAPIS_ORE
                || block == Blocks.EMERALD_ORE) {
            return 3;
        }

        return 0;
    }

    private static int resolveForestationValue(Block block) {
        if (block == Blocks.OAK_LOG
                || block == Blocks.SPRUCE_LOG
                || block == Blocks.BIRCH_LOG
                || block == Blocks.JUNGLE_LOG
                || block == Blocks.ACACIA_LOG
                || block == Blocks.DARK_OAK_LOG
                || block == Blocks.MANGROVE_LOG
                || block == Blocks.CHERRY_LOG
                || block == Blocks.BAMBOO_BLOCK) {
            return 2;
        }

        if (block == Blocks.OAK_SAPLING
                || block == Blocks.SPRUCE_SAPLING
                || block == Blocks.BIRCH_SAPLING
                || block == Blocks.JUNGLE_SAPLING
                || block == Blocks.ACACIA_SAPLING
                || block == Blocks.DARK_OAK_SAPLING
                || block == Blocks.CHERRY_SAPLING
                || block == Blocks.MANGROVE_PROPAGULE) {
            return 1;
        }

        return 0;
    }

    private static int resolvePollutionValue(Block block) {
        if (block == Blocks.FURNACE
                || block == Blocks.BLAST_FURNACE
                || block == Blocks.SMOKER
                || block == Blocks.CAMPFIRE
                || block == Blocks.SOUL_CAMPFIRE
                || block == Blocks.LAVA
                || block == Blocks.MAGMA_BLOCK) {
            return 2;
        }

        return 0;
    }
}
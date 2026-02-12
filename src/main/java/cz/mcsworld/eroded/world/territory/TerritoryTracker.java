package cz.mcsworld.eroded.world.territory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class TerritoryTracker {

    private TerritoryTracker() {}

    public static void onBlockPlaced(
            ServerWorld world,
            BlockPos pos,
            BlockState state
    ) {
        long tick = world.getServer().getTicks();

        TerritoryData data =
                TerritoryStorage.get(world, new ChunkPos(pos));

        Block block = state.getBlock();

        int forest = resolveForestationValue(block);
        int pollution = resolvePollutionValue(block);

        if (forest > 0) {
            data.addForestation(forest, tick);
        }

        if (pollution > 0) {
            data.addPollution(pollution, tick);
        }
    }

    public static void onBlockBroken(
            ServerWorld world,
            BlockPos pos,
            BlockState state
    ) {
        long tick = world.getServer().getTicks();

        TerritoryData data =
                TerritoryStorage.get(world, new ChunkPos(pos));

        int mining = resolveMiningValue(state, pos);
        if (mining > 0) {
            data.addMining(mining, tick);
        }
    }

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

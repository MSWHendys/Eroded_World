package cz.mcsworld.eroded.world.territory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class TerritoryTracker {

    private static final TagKey<Block> COAL_ORES =
            vanillaBlockTag("coal_ores");

    private static final TagKey<Block> REDSTONE_ORES =
            vanillaBlockTag("redstone_ores");

    private static final TagKey<Block> LAPIS_ORES =
            vanillaBlockTag("lapis_ores");

    private static final TagKey<Block> EMERALD_ORES =
            vanillaBlockTag("emerald_ores");

    private static final TagKey<Block> DIAMOND_ORES =
            vanillaBlockTag("diamond_ores");

    private static final TagKey<Block> SAPLINGS =
            vanillaBlockTag("saplings");

    private TerritoryTracker() {}

    private static TagKey<Block> vanillaBlockTag(String path) {
        return TagKey.create(
                Registries.BLOCK,
                Identifier.withDefaultNamespace(path)
        );
    }

    public static void onBlockPlaced(
            ServerLevel world,
            BlockPos pos,
            BlockState blockState
    ) {
        long tick = world.getServer().getTickCount();

        int forest = resolveForestationValue(blockState);
        int pollution = resolvePollutionValue(blockState);

        if (forest > 0 || pollution > 0) {
            updateCell(world, pos, cell -> {
                if (forest > 0) {
                    cell.addForestation(forest, tick);
                }

                if (pollution > 0) {
                    cell.addPollution(pollution, tick);
                }
            });
        }
    }

    public static void onBlockBroken(
            ServerLevel world,
            BlockPos pos,
            BlockState blockState
    ) {
        long tick = world.getServer().getTickCount();
        int mining = resolveMiningValue(blockState, pos);

        if (mining > 0) {
            updateCell(
                    world,
                    pos,
                    cell -> cell.addMining(mining, tick)
            );
        }
    }

    private static void updateCell(
            ServerLevel world,
            BlockPos pos,
            java.util.function.Consumer<TerritoryCell> action
    ) {
        TerritoryCellKey key = TerritoryCellKey.fromChunk(
                pos.getX() >> 4,
                pos.getZ() >> 4
        );

        TerritoryWorldState worldState =
                TerritoryWorldState.get(world);

        TerritoryCell cell =
                worldState.getOrCreateCell(key);

        action.accept(cell);
        worldState.setDirty();
    }

    private static int resolveMiningValue(
            BlockState state,
            BlockPos pos
    ) {
        if (state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(DIAMOND_ORES)
                || state.is(COAL_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(REDSTONE_ORES)
                || state.is(LAPIS_ORES)
                || state.is(EMERALD_ORES)) {

            return 3;
        }

        if (state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)) {

            return pos.getY() < 0 ? 2 : 1;
        }

        return 0;
    }

    private static int resolveForestationValue(BlockState state) {
        if (state.is(BlockTags.LOGS)) {
            return 2;
        }

        if (state.is(SAPLINGS)) {
            return 1;
        }

        return 0;
    }

    private static int resolvePollutionValue(BlockState state) {
        if (state.is(BlockTags.CAMPFIRES)
                || state.is(BlockTags.FIRE)) {

            return 2;
        }

        Block block = state.getBlock();

        if (block == net.minecraft.world.level.block.Blocks.FURNACE
                || block == net.minecraft.world.level.block.Blocks.BLAST_FURNACE
                || block == net.minecraft.world.level.block.Blocks.SMOKER
                || block == net.minecraft.world.level.block.Blocks.LAVA
                || block == net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) {

            return 2;
        }

        return 0;
    }
}
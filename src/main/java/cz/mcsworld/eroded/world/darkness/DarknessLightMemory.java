package cz.mcsworld.eroded.world.darkness;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class DarknessLightMemory {

    private static final Map<BlockPos, BlockState> ORIGINAL = new HashMap<>();

    private DarknessLightMemory() {}

    public static boolean has(BlockPos pos) {
        return ORIGINAL.containsKey(pos);
    }

    public static void store(BlockPos pos, BlockState state) {
        ORIGINAL.putIfAbsent(pos.toImmutable(), state);
    }

    public static BlockState get(BlockPos pos) {
        return ORIGINAL.get(pos);
    }

    public static void clear(BlockPos pos) {
        ORIGINAL.remove(pos);
    }
}

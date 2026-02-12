package cz.mcsworld.eroded.world.darkness;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class DarknessFlickerState {

    public static final int MAX_STAGE = 3;

    private static final Map<BlockPos, Integer> STAGES = new HashMap<>();

    private DarknessFlickerState() {}

    public static int getStage(BlockPos pos) {
        return STAGES.getOrDefault(pos, 0);
    }

    public static boolean advance(BlockPos pos) {
        int next = STAGES.getOrDefault(pos, 0) + 1;

        if (next >= MAX_STAGE) {
            STAGES.remove(pos);
            return true;
        }

        STAGES.put(pos.toImmutable(), next);
        return false;
    }

    public static void clear(BlockPos pos) {
        STAGES.remove(pos);
    }
}

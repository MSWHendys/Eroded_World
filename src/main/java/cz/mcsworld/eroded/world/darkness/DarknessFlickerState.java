package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;


public final class DarknessFlickerState {


    private static final Map<BlockPos, Integer> STAGES = new HashMap<>();

    private DarknessFlickerState() {}

    public static int getStage(BlockPos pos) {
        return STAGES.getOrDefault(pos, 0);
    }

    public static boolean advance(BlockPos pos) {
        var cfg = DarknessConfigs.get().server;
        int next = STAGES.getOrDefault(pos, 0) + 1;

        if (next >= cfg.flickerStages) {
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

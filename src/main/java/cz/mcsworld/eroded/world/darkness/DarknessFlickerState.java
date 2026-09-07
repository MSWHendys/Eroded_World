package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class DarknessFlickerState {

    /**
     * Flicker state is deliberately bounded. A light can disappear because a
     * player breaks/replaces it or because its chunk unloads before the next
     * Light Eater pass, so this cache must never be allowed to grow without a
     * hard limit.
     */
    private static final int MAX_ENTRIES = 1000;

    private static final Map<LightKey, Integer> STAGES = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<LightKey, Integer> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private DarknessFlickerState() {}

    private record LightKey(ResourceKey<Level> dimension, BlockPos pos) {
        private static LightKey of(ServerLevel world, BlockPos pos) {
            return new LightKey(world.dimension(), pos.immutable());
        }
    }

    public static boolean has(ServerLevel world, BlockPos pos) {
        return STAGES.containsKey(LightKey.of(world, pos));
    }

    public static int getStage(ServerLevel world, BlockPos pos) {
        return STAGES.getOrDefault(LightKey.of(world, pos), 0);
    }

    public static boolean advance(ServerLevel world, BlockPos pos) {
        var cfg = DarknessConfigs.get().server;
        LightKey key = LightKey.of(world, pos);
        int next = STAGES.getOrDefault(key, 0) + 1;

        if (next >= cfg.flickerStages) {
            STAGES.remove(key);
            return true;
        }

        STAGES.put(key, next);
        return false;
    }

    public static void clear(ServerLevel world, BlockPos pos) {
        STAGES.remove(LightKey.of(world, pos));
    }

    public static void clearAll() {
        STAGES.clear();
    }

    static int sizeForDebug() {
        return STAGES.size();
    }
}

package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.monster.Monster;

public final class DarknessMobLightMemory {

    private static final Map<Monster, Integer> MEMORY = new WeakHashMap<>();

    private DarknessMobLightMemory() {}

    public static void markLightExtinguished(Monster mob) {
        var cfg = DarknessConfigs.get().server;
        MEMORY.put(mob, cfg.postLightCooldownTicks);
    }

    public static boolean isInPostLightPause(Monster mob) {
        Integer ticks = MEMORY.get(mob);
        if (ticks == null) return false;

        if (ticks <= 0) {
            MEMORY.remove(mob);
            return false;
        }

        MEMORY.put(mob, ticks - 1);
        return true;
    }
}

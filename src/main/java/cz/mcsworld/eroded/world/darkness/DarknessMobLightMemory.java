package cz.mcsworld.eroded.world.darkness;

import net.minecraft.entity.mob.HostileEntity;

import java.util.Map;
import java.util.WeakHashMap;

public final class DarknessMobLightMemory {

    private static final int COOLDOWN_TICKS = 12;
    private static final Map<HostileEntity, Integer> MEMORY = new WeakHashMap<>();

    private DarknessMobLightMemory() {}

    public static void markLightExtinguished(HostileEntity mob) {
        MEMORY.put(mob, COOLDOWN_TICKS);
    }

    public static boolean isInPostLightPause(HostileEntity mob) {
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

package cz.mcsworld.eroded.world.darkness;

import com.mojang.serialization.Codec;
import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent ownership marker for invisible LIGHT blocks created by Eroded.
 * The block itself lives in the world save; this state lets us distinguish it
 * from map-maker / command / other-mod LIGHT blocks after a restart.
 */
public final class DynamicLightState extends SavedData {

    private final Map<Long, Integer> managedLevels;

    public DynamicLightState() {
        this(new HashMap<>());
    }

    private DynamicLightState(Map<Long, Integer> managedLevels) {
        this.managedLevels = managedLevels;
    }

    private static final Codec<DynamicLightState> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(
                    raw -> {
                        Map<Long, Integer> decoded = new HashMap<>();
                        raw.forEach((key, value) -> {
                            try {
                                decoded.put(Long.parseLong(key), value);
                            } catch (NumberFormatException ignored) {
                            }
                        });
                        return new DynamicLightState(decoded);
                    },
                    state -> {
                        Map<String, Integer> out = new HashMap<>();
                        state.managedLevels.forEach((pos, level) -> out.put(Long.toString(pos), level));
                        return out;
                    }
            );

    public static final SavedDataType<DynamicLightState> TYPE = new SavedDataType<>(
            ErodedMod.MOD_ID + "_dynamic_lights",
            DynamicLightState::new,
            CODEC,
            null
    );

    public static DynamicLightState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    public static DynamicLightState getIfPresent(ServerLevel world) {
        return world.getDataStorage().get(TYPE);
    }

    public boolean isManaged(long pos) {
        return managedLevels.containsKey(pos);
    }

    public int expectedLevel(long pos) {
        return managedLevels.getOrDefault(pos, -1);
    }

    public void mark(long pos, int level) {
        Integer previous = managedLevels.put(pos, level);
        if (previous == null || previous != level) setDirty();
    }

    public void unmark(long pos) {
        if (managedLevels.remove(pos) != null) setDirty();
    }

    public Map<Long, Integer> snapshot() {
        return Map.copyOf(managedLevels);
    }
}

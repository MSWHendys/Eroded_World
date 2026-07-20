package cz.mcsworld.eroded.energy;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class EnergyPersistentState extends SavedData {

    private static final String ID = "eroded_energy";

    private final Map<UUID, Integer> energy = new HashMap<>();

    private static final Codec<EnergyPersistentState> CODEC =
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    Codec.INT
            ).fieldOf("energy").codec().xmap(
                    map -> {
                        EnergyPersistentState state = new EnergyPersistentState();
                        state.energy.putAll(map);
                        return state;
                    },
                    state -> state.energy
            );

    public static final SavedDataType<EnergyPersistentState> TYPE =
            new SavedDataType<>(
                    ID,
                    EnergyPersistentState::new,
                    CODEC,
                    null
            );

    public static EnergyPersistentState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    public int getEnergy(UUID uuid, int fallback) {
        return energy.getOrDefault(uuid, fallback);
    }

    public void setEnergy(UUID uuid, int value) {
        energy.put(uuid, value);
        setDirty();
    }
}
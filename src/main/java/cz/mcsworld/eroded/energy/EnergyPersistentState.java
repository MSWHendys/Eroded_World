package cz.mcsworld.eroded.energy;

import com.mojang.serialization.Codec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EnergyPersistentState extends PersistentState {

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

    public static final PersistentStateType<EnergyPersistentState> TYPE =
            new PersistentStateType<>(
                    ID,
                    EnergyPersistentState::new,
                    CODEC,
                    null
            );

    public static EnergyPersistentState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public int getEnergy(UUID uuid, int fallback) {
        return energy.getOrDefault(uuid, fallback);
    }

    public void setEnergy(UUID uuid, int value) {
        energy.put(uuid, value);
        markDirty();
    }
}
package cz.mcsworld.eroded.energy;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EnergyPersistentState extends SavedData {

    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("eroded", "energy");

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

    public static final SavedDataType<@NotNull EnergyPersistentState> TYPE =
            new SavedDataType<>(
                    ID,
                    EnergyPersistentState::new,
                    CODEC,
                    DataFixTypes.LEVEL
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
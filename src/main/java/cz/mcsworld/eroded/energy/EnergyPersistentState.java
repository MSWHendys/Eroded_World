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

/**
 * Legacy energy-only persistence from versions before Part 8A.
 *
 * <p>Energy is now authoritative in SkillPersistentState. This SavedData is
 * retained only so existing worlds can migrate a player's last value once,
 * without losing progress.</p>
 */
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

    public static EnergyPersistentState getIfPresent(ServerLevel world) {
        return world.getDataStorage().get(TYPE);
    }

    /**
     * Removes and returns one legacy energy value. Once consumed, future
     * logins use SkillPersistentState as the only source of truth.
     */
    public Integer takeEnergy(UUID uuid) {
        Integer value = energy.remove(uuid);
        if (value != null) {
            setDirty();
        }
        return value;
    }

    public int size() {
        return energy.size();
    }
}

package cz.mcsworld.eroded.skills;

import com.mojang.serialization.Codec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkillPersistentState extends PersistentState {

    private static final String ID = "eroded_skills";

    private final Map<UUID, SkillDataRecord> players = new HashMap<>();

    private static final Codec<SkillPersistentState> CODEC =
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    SkillDataRecord.CODEC
            ).fieldOf("players").codec().xmap(
                    map -> {
                        SkillPersistentState state = new SkillPersistentState();
                        state.players.putAll(map);
                        return state;
                    },
                    state -> state.players
            );

    public static final PersistentStateType<SkillPersistentState> TYPE =
            new PersistentStateType<>(
                    ID,
                    SkillPersistentState::new,
                    CODEC,
                    null
            );

    public static SkillPersistentState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }


    public SkillData getOrCreate(UUID uuid) {

        SkillDataRecord record = players.get(uuid);

        SkillData data = new SkillData();

        if (record != null) {
            SkillDataRecord.applyToSkillData(data, record);
        }

        return data;
    }

    public void save(UUID uuid, SkillData data) {
        players.put(uuid, SkillDataRecord.fromSkillData(data));
        markDirty();
    }
}
package cz.mcsworld.eroded.skills;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class SkillPersistentState extends SavedData {

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

    public static final SavedDataType<SkillPersistentState> TYPE =
            new SavedDataType<>(
                    ID,
                    SkillPersistentState::new,
                    CODEC,
                    null
            );

    public static SkillPersistentState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
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
        setDirty();
    }
}
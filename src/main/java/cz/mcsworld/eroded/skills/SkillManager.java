package cz.mcsworld.eroded.skills;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillManager {


    private static final Map<UUID, SkillData> CACHE = new HashMap<>();

    public static SkillData get(ServerPlayerEntity player) {

        UUID uuid = player.getUuid();

        if (CACHE.containsKey(uuid)) {
            return CACHE.get(uuid);
        }

        SkillPersistentState state =
                SkillPersistentState.get(player.getWorld());

        SkillData data = state.getOrCreate(uuid);

        CACHE.put(uuid, data);

        return data;
    }

    public static void save(ServerPlayerEntity player) {

        UUID uuid = player.getUuid();

        SkillData data = CACHE.get(uuid);
        if (data == null) return;

        SkillPersistentState state =
                SkillPersistentState.get(player.getWorld());

        state.save(uuid, data);
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }
}
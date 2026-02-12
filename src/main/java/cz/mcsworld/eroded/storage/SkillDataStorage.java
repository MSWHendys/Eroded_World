package cz.mcsworld.eroded.storage;

import cz.mcsworld.eroded.skills.SkillData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillDataStorage {

    private static final Map<UUID, SkillData> DATA = new ConcurrentHashMap<>();

    private SkillDataStorage() {}

    public static SkillData getOrCreate(UUID uuid) {
        return DATA.computeIfAbsent(uuid, id -> {
            SkillData d = new SkillData();
            d.initialize();
            return d;
        });
    }

    public static void remove(UUID uuid) {
        DATA.remove(uuid);
    }

    public static Map<UUID, SkillData> getAll() {
        return DATA;
    }
}

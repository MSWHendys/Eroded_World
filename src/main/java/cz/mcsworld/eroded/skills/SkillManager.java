package cz.mcsworld.eroded.skills;

import cz.mcsworld.eroded.storage.SkillDataStorage;
import net.minecraft.server.network.ServerPlayerEntity;

public class SkillManager {

    public static SkillData get(ServerPlayerEntity player) {
        return SkillDataStorage.getOrCreate(player.getUuid());
    }

}

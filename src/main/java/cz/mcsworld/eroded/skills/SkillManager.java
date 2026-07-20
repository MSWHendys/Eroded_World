package cz.mcsworld.eroded.skills;

import cz.mcsworld.eroded.network.EnergySyncPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillManager {

    private static final Map<UUID, SkillData> CACHE = new HashMap<>();

    private static ServerLevel storageWorld(ServerPlayer player) {
        return player.getServer().overworld();
    }

    public static SkillData get(ServerPlayer player) {
        UUID uuid = player.getUUID();

        SkillData cached = CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }

        SkillPersistentState state = SkillPersistentState.get(storageWorld(player));
        SkillData data = state.getOrCreate(uuid);

        CACHE.put(uuid, data);
        return data;
    }

    public static void sync(ServerPlayer player) {
        SkillData data = get(player);
        int remainingImmunity = (int) (data.getImmunityRemainingMs() / 1000);

        EnergySyncPacket packet = new EnergySyncPacket(
                data.getEnergy(),
                data.getMaxEnergy(),
                remainingImmunity
        );

        ServerPlayNetworking.send(player, packet);
    }

    public static void save(ServerPlayer player) {
        UUID uuid = player.getUUID();
        SkillData data = CACHE.get(uuid);

        if (data == null) {
            return;
        }

        SkillPersistentState state = SkillPersistentState.get(storageWorld(player));
        state.save(uuid, data);

        sync(player);
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }
}
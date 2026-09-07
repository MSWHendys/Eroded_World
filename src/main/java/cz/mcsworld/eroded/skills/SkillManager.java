package cz.mcsworld.eroded.skills;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.energy.EnergySyncHandler;
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
        return player.level().getServer().overworld();
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

    /**
     * Force an Energy sync and update the server-side last-sent cache.
     * Use save() for normal mutations; use sync() only when a forced refresh
     * is actually needed (login/config toggle/manual refresh).
     */
    public static void sync(ServerPlayer player) {
        EnergySyncHandler.forceSync(player);
    }

    /**
     * Raw packet sender used only by EnergySyncHandler. Keeping the last-sent
     * bookkeeping there prevents a normal save from being sent again by the
     * end-of-tick change detector.
     */
    public static void sendEnergySyncPacket(ServerPlayer player) {
        SkillData data = get(player);
        int remainingImmunity = (int) (data.getImmunityRemainingMs() / 1000);

        var server = EnergyConfig.get().server;
        boolean enabled = server.enabled;
        var mining = server.mining;

        EnergySyncPacket packet = new EnergySyncPacket(
                enabled,
                data.getEnergy(),
                data.getMaxEnergy(),
                enabled ? remainingImmunity : 0,
                mining.enabled,
                mining.speedScalingEnabled,
                mining.allowMiningAtZero,
                mining.fullSpeedFromPercent,
                mining.reducedSpeedFromPercent,
                mining.reducedSpeedPercent,
                mining.criticalSpeedPercent
        );

        ServerPlayNetworking.send(player, packet);
    }

    public static void persist(ServerPlayer player) {
        UUID uuid = player.getUUID();
        SkillData data = CACHE.get(uuid);

        if (data == null) {
            return;
        }

        SkillPersistentState state = SkillPersistentState.get(storageWorld(player));
        state.save(uuid, data);
    }

    public static void save(ServerPlayer player) {
        persist(player);
        EnergySyncHandler.syncIfChanged(player, false);
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }
}

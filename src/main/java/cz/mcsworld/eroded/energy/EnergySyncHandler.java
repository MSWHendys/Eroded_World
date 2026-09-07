package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.network.EnergyWarningPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnergySyncHandler {

    private record LastEnergySync(
            boolean enabled,
            int energy,
            int maxEnergy,
            int immunitySeconds,
            boolean miningEnabled,
            boolean miningSpeedScalingEnabled,
            boolean miningAllowAtZero,
            int miningFullSpeedFromPercent,
            int miningReducedSpeedFromPercent,
            int miningReducedSpeedPercent,
            int miningCriticalSpeedPercent
    ) {
    }

    private static final Map<UUID, LastEnergySync> LAST_SENT = new ConcurrentHashMap<>();

    private EnergySyncHandler() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            SkillData data = SkillManager.get(player);

            migrateLegacyEnergy(player, data);

            // Persist once after loading. This upgrades old SkillData records
            // to the Part 8B schema and commits any expired collapse/immunity
            // normalization performed while the record was restored.
            SkillManager.persist(player);
            forceSync(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LAST_SENT.remove(handler.getPlayer().getUUID())
        );

        ServerTickEvents.END_SERVER_TICK.register(EnergySyncHandler::tick);
    }

    /**
     * Part 8A migration: old versions stored energy twice.  The legacy
     * eroded_energy value was the one that won on login, so preserve that
     * behaviour exactly once and move the value into eroded_skills.
     */
    private static void migrateLegacyEnergy(ServerPlayer player, SkillData data) {
        var legacy = EnergyPersistentState.getIfPresent(player.getServer().overworld());
        if (legacy == null) return;

        Integer legacyValue = legacy.takeEnergy(player.getUUID());
        if (legacyValue == null) return;

        data.setEnergy(legacyValue);

        ErodedMod.LOGGER.debug(
                "[Eroded World] Migrated legacy energy persistence for {} ({} remaining legacy entries).",
                player.getGameProfile().getName(),
                legacy.size()
        );
    }

    private static void tick(MinecraftServer server) {
        var root = EnergyConfig.get();
        boolean enabled = root.server.enabled;
        var cfg = root.server.warnings;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SkillData data = SkillManager.get(player);

            // Do not let time spent with the master switch disabled become a
            // giant passive-regeneration catch-up when Energy is re-enabled.
            if (!enabled) {
                data.pauseRegenerationClock();
            }

            syncIfChanged(player);

            if (!enabled) continue;
            if (!cfg.warningsEnabled) continue;

            // Warning transitions have their own state in SkillData and must
            // not depend on whether an Energy packet happened to be sent by
            // another gameplay handler earlier in the same tick.
            SkillData.EnergyState worse = data.detectWorseningState();
            if (worse == null) continue;

            SafeNetworkUtil.safeSend(player, new EnergyWarningPacket(worse));
        }
    }

    public static boolean syncIfChanged(ServerPlayer player) {
        return syncIfChanged(player, true);
    }

    /**
     * Sends the Energy payload only when its externally visible state changed.
     * The persistence flag is false when the caller has already persisted the
     * whole SkillData record (SkillManager.save), avoiding a duplicate state
     * write/dirty mark in the same mutation path.
     */
    public static boolean syncIfChanged(ServerPlayer player, boolean persistEnergyChange) {
        SkillData data = SkillManager.get(player);
        var serverCfg = EnergyConfig.get().server;
        boolean enabled = serverCfg.enabled;
        var miningCfg = serverCfg.mining;

        int energy = data.getEnergy();
        int currentImmunity = enabled
                ? (int) (data.getImmunityRemainingMs() / 1000)
                : 0;

        LastEnergySync current = new LastEnergySync(
                enabled,
                energy,
                data.getMaxEnergy(),
                currentImmunity,
                miningCfg.enabled,
                miningCfg.speedScalingEnabled,
                miningCfg.allowMiningAtZero,
                miningCfg.fullSpeedFromPercent,
                miningCfg.reducedSpeedFromPercent,
                miningCfg.reducedSpeedPercent,
                miningCfg.criticalSpeedPercent
        );
        LastEnergySync last = LAST_SENT.get(player.getUUID());

        if (current.equals(last)) {
            return false;
        }

        LAST_SENT.put(player.getUUID(), current);

        // SkillPersistentState is the only authoritative persistence. Persist
        // numeric Energy changes discovered by the passive end-of-tick sync;
        // callers that just used SkillManager.save() pass false here.
        if (persistEnergyChange && (last == null || current.energy() != last.energy())) {
            SkillManager.persist(player);
        }

        SkillManager.sendEnergySyncPacket(player);
        return true;
    }

    public static void forceSync(ServerPlayer player) {
        SkillData data = SkillManager.get(player);
        var serverCfg = EnergyConfig.get().server;
        boolean enabled = serverCfg.enabled;
        var miningCfg = serverCfg.mining;

        int energy = data.getEnergy();
        int currentImmunity = enabled
                ? (int) (data.getImmunityRemainingMs() / 1000)
                : 0;

        LAST_SENT.put(
                player.getUUID(),
                new LastEnergySync(
                        enabled,
                        energy,
                        data.getMaxEnergy(),
                        currentImmunity,
                        miningCfg.enabled,
                        miningCfg.speedScalingEnabled,
                        miningCfg.allowMiningAtZero,
                        miningCfg.fullSpeedFromPercent,
                        miningCfg.reducedSpeedFromPercent,
                        miningCfg.reducedSpeedPercent,
                        miningCfg.criticalSpeedPercent
                )
        );

        SkillManager.sendEnergySyncPacket(player);
    }
}

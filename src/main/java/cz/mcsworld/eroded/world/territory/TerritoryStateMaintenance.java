package cz.mcsworld.eroded.world.territory;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;

/** Periodic, chunk-load-free maintenance for persistent territory state. */
public final class TerritoryStateMaintenance {

    private TerritoryStateMaintenance() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(TerritoryStateMaintenance::onWorldTick);
    }

    private static void onWorldTick(ServerLevel world) {
        var cfg = TerritoryConfig.get().server;
        if (!cfg.enabled || !cfg.statePruningEnabled) {
            return;
        }

        long now = world.getGameTime();
        if (now <= 0L || now % cfg.statePruneIntervalTicks != 0L) {
            return;
        }

        TerritoryWorldState state = TerritoryWorldState.getIfPresent(world);
        if (state == null || !state.hasCells()) {
            return;
        }

        int before = state.size();
        int removed = state.pruneInactive(now, cfg.stateRetentionTicks);
        if (removed > 0) {
            ErodedMod.LOGGER.debug(
                    "[Eroded World] Territory maintenance pruned {} inactive cells in {} ({} -> {}).",
                    removed,
                    world.dimension(),
                    before,
                    state.size()
            );
        }
    }
}

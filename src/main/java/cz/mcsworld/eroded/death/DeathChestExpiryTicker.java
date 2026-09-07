package cz.mcsworld.eroded.death;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;

public final class DeathChestExpiryTicker {

    private DeathChestExpiryTicker() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(DeathChestExpiryTicker::tick);
    }

    private static void tick(ServerLevel world) {
        // Expiry is wall-clock based and does not need a 20 Hz full state scan.
        if (world.getGameTime() % 20L != 0L) return;

        long now = System.currentTimeMillis();
        DeathChestState state = DeathChestState.getIfPresent(world);
        if (state == null) return;
        List<DeathChestState.Entry> expired = new ArrayList<>();

        for (DeathChestState.Entry entry : state.all()) {
            // Never mutate persistent remains in an unloaded chunk. After a
            // restart, SavedData is available before distant chunks are loaded;
            // removing the SavedData entry here could leave a physical orphan
            // chest in the region file with no owner/items and therefore no way
            // to open it later. Expiry is completed once the chunk is loaded.
            if (!world.hasChunkAt(entry.pos())) {
                continue;
            }

            // Never expire a container underneath an active GUI. The menu close
            // path will atomically materialize the remaining stacks once.
            if (state.isOpen(entry.pos())) {
                continue;
            }

            if (now >= entry.protectUntilEpochMs()) {
                expired.add(entry);
            }
        }

        for (DeathChestState.Entry entry : expired) {
            BlockPos pos = entry.pos();

            // Re-check because another server-thread action may have opened it
            // between collection and processing in future refactors.
            if (state.isOpen(pos) || state.get(pos) != entry) {
                continue;
            }

            for (var stored : entry.items().values()) {
                if (stored.stack().isEmpty()) continue;

                ItemEntity item = new ItemEntity(
                        world,
                        pos.getX() + 0.5,
                        pos.getY() + 1.5,
                        pos.getZ() + 0.5,
                        stored.stack().copy()
                );
                world.addFreshEntity(item);
            }

            world.destroyBlock(pos, false);
            DeathHologramHandler.removeAt(world, pos, entry.hologramId());
            state.remove(pos);
        }
    }
}

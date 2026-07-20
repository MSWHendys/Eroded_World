package cz.mcsworld.eroded.death;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import java.util.ArrayList;
import java.util.List;

public final class DeathChestExpiryTicker {

    private DeathChestExpiryTicker() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(DeathChestExpiryTicker::tick);
    }

    private static void tick(ServerLevel world) {

        long now = System.currentTimeMillis();
        DeathChestState state = DeathChestState.get(world);

        List<DeathChestState.Entry> expired = new ArrayList<>();

        for (DeathChestState.Entry entry : state.all()) {
            if (now >= entry.protectUntilEpochMs()) {
                expired.add(entry);
            }
        }

        for (DeathChestState.Entry entry : expired) {

            BlockPos pos = entry.pos();

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

            boolean hadBlock =
                    !world.getBlockState(pos).isAir();

            world.destroyBlock(pos, false);

            DeathHologramHandler.removeById(
                    world,
                    entry.hologramId()
            );

            state.remove(pos);

        }
    }
}

package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class DeathChestParticles {

    private static int tick = 0;

    private DeathChestParticles() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DeathChestParticles::onTick);
    }

    private static void onTick(MinecraftServer server) {
        tick++;

        // Particles are emitted only twice per second, so scanning every death
        // chest on the nine ticks in between is wasted work.
        if (tick % 10 != 0) {
            return;
        }

        for (ServerLevel world : server.getAllLevels()) {
            DeathChestState state = DeathChestState.getIfPresent(world);
            if (state == null) {
                continue;
            }

            var entries = state.all();
            if (entries.isEmpty()) {
                continue;
            }

            List<BlockPos> stale = new ArrayList<>();

            for (DeathChestState.Entry e : entries) {
                BlockPos pos = e.pos();

                // Never load a remote chunk just to validate/particle a death
                // chest. Its physical block is checked once the chunk is loaded.
                if (!world.hasChunkAt(pos)) {
                    continue;
                }

                if (!world.getBlockState(pos).is(ErodedBlocks.DEATH_ENDER_CHEST)) {
                    DeathHologramHandler.removeAt(world, pos, e.hologramId());
                    stale.add(pos);
                    continue;
                }

                if (state.isProtected(pos)) {
                    world.sendParticles(
                            ParticleTypes.SMOKE,
                            pos.getX() + 0.5,
                            pos.getY() + 1.05,
                            pos.getZ() + 0.5,
                            2,
                            0.2, 0.05, 0.2,
                            0.0
                    );
                }
            }

            for (BlockPos pos : stale) {
                state.remove(pos);
            }
        }
    }
}

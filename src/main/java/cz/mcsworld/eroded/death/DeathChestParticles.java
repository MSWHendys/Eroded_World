package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class DeathChestParticles {

    private static int tick = 0;

    private DeathChestParticles() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DeathChestParticles::onTick);
    }

    private static void onTick(MinecraftServer server) {
        tick++;

        for (ServerWorld world : server.getWorlds()) {
            DeathChestState state = DeathChestState.get(world);

            var it = state.all().iterator();
            while (it.hasNext()) {
                DeathChestState.Entry e = it.next();
                BlockPos pos = e.pos();

                if (!world.getBlockState(pos).isOf(ErodedBlocks.DEATH_ENDER_CHEST)) {

                    DeathHologramHandler.removeById(
                            world,
                            e.hologramId()
                    );

                    it.remove();
                    state.markDirty();

                    continue;
                }

                if (!world.isChunkLoaded(pos)) continue;

                if (tick % 10 == 0 && state.isProtected(pos)) {
                    world.spawnParticles(
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
        }
    }
}

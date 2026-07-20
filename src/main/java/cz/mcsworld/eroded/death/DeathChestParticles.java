package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
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

        for (ServerLevel world : server.getAllLevels()) {
            DeathChestState state = DeathChestState.get(world);

            var it = state.all().iterator();
            while (it.hasNext()) {
                DeathChestState.Entry e = it.next();
                BlockPos pos = e.pos();

                if (!world.getBlockState(pos).is(ErodedBlocks.DEATH_ENDER_CHEST)) {

                    DeathHologramHandler.removeById(
                            world,
                            e.hologramId()
                    );

                    it.remove();
                    state.setDirty();

                    continue;
                }

                if (!world.hasChunkAt(pos)) continue;

                if (tick % 10 == 0 && state.isProtected(pos)) {
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
        }
    }
}

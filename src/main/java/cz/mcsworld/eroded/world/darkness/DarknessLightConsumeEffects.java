package cz.mcsworld.eroded.world.darkness;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.ParticleTypes;

public final class DarknessLightConsumeEffects {

    private DarknessLightConsumeEffects() {}

    public static void play(ServerWorld world, BlockPos pos) {

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        world.playSound(
                null,
                x, y, z,
                SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.HOSTILE,
                0.6f,
                0.6f + world.getRandom().nextFloat() * 0.2f
        );

        world.spawnParticles(
                ParticleTypes.SMOKE,
                x, y + 0.2, z,
                8,
                0.15, 0.15, 0.15,
                0.01
        );

        world.spawnParticles(
                ParticleTypes.ASH,
                x, y + 0.2, z,
                6,
                0.1, 0.1, 0.1,
                0.005
        );
    }
}

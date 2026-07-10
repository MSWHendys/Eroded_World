package cz.mcsworld.eroded.world.darkness;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class DarknessLightConsumeEffects {

    private DarknessLightConsumeEffects() {}

    public static void play(ServerLevel world, BlockPos pos) {

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        world.playSound(
                null,
                x, y, z,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.HOSTILE,
                0.6f,
                0.6f + world.getRandom().nextFloat() * 0.2f
        );

        world.sendParticles(
                ParticleTypes.SMOKE,
                x, y + 0.2, z,
                8,
                0.15, 0.15, 0.15,
                0.01
        );

        world.sendParticles(
                ParticleTypes.ASH,
                x, y + 0.2, z,
                6,
                0.1, 0.1, 0.1,
                0.005
        );
    }
}

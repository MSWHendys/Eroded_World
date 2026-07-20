package cz.mcsworld.eroded.world.darkness;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class DarknessFlickerEffects {

    private DarknessFlickerEffects() {}

    public static void play(ServerLevel world, BlockPos pos) {

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        world.playSound(
                null,
                x, y, z,
                SoundEvents.REDSTONE_TORCH_BURNOUT,
                SoundSource.HOSTILE,
                0.4f,
                0.9f + world.getRandom().nextFloat() * 0.2f
        );
    }
}

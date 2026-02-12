package cz.mcsworld.eroded.world.darkness;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public final class DarknessFlickerEffects {

    private DarknessFlickerEffects() {}

    public static void play(ServerWorld world, BlockPos pos) {

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        world.playSound(
                null,
                x, y, z,
                SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT,
                SoundCategory.HOSTILE,
                0.4f,
                0.9f + world.getRandom().nextFloat() * 0.2f
        );
    }
}

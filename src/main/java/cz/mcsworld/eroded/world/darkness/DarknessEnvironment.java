package cz.mcsworld.eroded.world.darkness;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

public final class DarknessEnvironment {

    private DarknessEnvironment() {}

    public static boolean isDarkForMobs(ServerWorld world, BlockPos pos) {
        int sky = world.getLightLevel(LightType.SKY, pos);
        int block = world.getLightLevel(LightType.BLOCK, pos);

        return sky == 0 && block <= 7;
    }

    public static boolean isNight(ServerWorld world) {
        long t = world.getTimeOfDay() % 24000;
        return t >= 13000 && t <= 23000;
    }
}

package cz.mcsworld.eroded.world.darkness;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;

public final class DarknessEnvironment {

    private DarknessEnvironment() {}

    public static boolean isDarkForMobs(ServerLevel world, BlockPos pos) {
        int sky = world.getBrightness(LightLayer.SKY, pos);
        int block = world.getBrightness(LightLayer.BLOCK, pos);

        return sky == 0 && block <= 7;
    }

    public static boolean isNight(ServerLevel world) {
        long t = world.getDayTime() % 24000;
        return t >= 13000 && t <= 23000;
    }
}

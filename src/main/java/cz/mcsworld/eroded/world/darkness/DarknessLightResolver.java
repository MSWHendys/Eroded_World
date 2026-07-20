package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public final class DarknessLightResolver {

    private DarknessLightResolver() {}

    public static final int FEAR_LIGHT_THRESHOLD = 4;

    public static BlockPos findNearbyBlockLight(ServerLevel world, BlockPos origin) {
        var cfg = DarknessConfigs.get().server;


        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -cfg.lightSearchRadius; dx <= cfg.lightSearchRadius; dx++) {
            for (int dy = -cfg.lightSearchRadius; dy <= cfg.lightSearchRadius; dy++) {
                for (int dz = -cfg.lightSearchRadius; dz <= cfg.lightSearchRadius; dz++) {

                    pos.set(
                            origin.getX() + dx,
                            origin.getY() + dy,
                            origin.getZ() + dz
                    );

                    int block = world.getBrightness(LightLayer.BLOCK, pos);
                    if (block < cfg.fearLightThreshold) continue;

                    double d = pos.distSqr(origin);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos.immutable();
                    }
                }
            }
        }

        return best;
    }

    public static Vec3 escapeFrom(BlockPos mobPos, BlockPos lightPos) {

        Vec3 dir = Vec3.atCenterOf(mobPos)
                .subtract(Vec3.atCenterOf(lightPos));

        Vec3 flat = new Vec3(dir.x, 0, dir.z);

        if (flat.lengthSqr() < 0.0001) {
            return Vec3.ZERO;
        }

        return flat.normalize();
    }

    public static boolean isMobFearing(ServerLevel world, BlockPos pos) {
        var cfg = DarknessConfigs.get().server;
        return world.getBrightness(LightLayer.BLOCK, pos) >= cfg.fearLightThreshold;
    }

    public static boolean isMobSuppressed(ServerLevel world, BlockPos pos) {
        var cfg = DarknessConfigs.get().server;
        return world.getBrightness(LightLayer.BLOCK, pos) >= cfg.suppressLightThreshold;
    }
}

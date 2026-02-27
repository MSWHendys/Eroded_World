package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

public final class DarknessLightResolver {

    private DarknessLightResolver() {}

    public static final int FEAR_LIGHT_THRESHOLD = 4;

    public static BlockPos findNearbyBlockLight(ServerWorld world, BlockPos origin) {
        var cfg = DarknessConfigs.get().server;


        BlockPos.Mutable pos = new BlockPos.Mutable();
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

                    int block = world.getLightLevel(LightType.BLOCK, pos);
                    if (block < cfg.fearLightThreshold) continue;

                    double d = pos.getSquaredDistance(origin);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos.toImmutable();
                    }
                }
            }
        }

        return best;
    }

    public static Vec3d escapeFrom(BlockPos mobPos, BlockPos lightPos) {

        Vec3d dir = Vec3d.ofCenter(mobPos)
                .subtract(Vec3d.ofCenter(lightPos));

        Vec3d flat = new Vec3d(dir.x, 0, dir.z);

        if (flat.lengthSquared() < 0.0001) {
            return Vec3d.ZERO;
        }

        return flat.normalize();
    }

    public static boolean isMobFearing(ServerWorld world, BlockPos pos) {
        var cfg = DarknessConfigs.get().server;
        return world.getLightLevel(LightType.BLOCK, pos) >= cfg.fearLightThreshold;
    }

    public static boolean isMobSuppressed(ServerWorld world, BlockPos pos) {
        var cfg = DarknessConfigs.get().server;
        return world.getLightLevel(LightType.BLOCK, pos) >= cfg.suppressLightThreshold;
    }
}

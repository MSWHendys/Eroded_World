package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import java.util.EnumSet;

public final class EscapeFromLightGoal extends Goal {
    private final HostileEntity mob;
    private Vec3d targetPos;
    private final double speed;

    public EscapeFromLightGoal(HostileEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {

        if (!mob.isAlive() || mob.getTarget() != null) return false;

        ServerWorld world = (ServerWorld) mob.getWorld();
        BlockPos pos = mob.getBlockPos();
        var cfg = DarknessConfigs.get().server;
        if (!DarknessEnvironment.isDarkForMobs(world, pos)) return false;
        if (world.getLightLevel(LightType.BLOCK, pos) < cfg.fearLightThreshold) return false;

        this.targetPos = findDarkPlace(world, pos);
        return this.targetPos != null;
    }

    private Vec3d findDarkPlace(ServerWorld world, BlockPos origin) {
        var cfg = DarknessConfigs.get().server;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int i = 0; i < 15; i++) {
            int x = origin.getX() + world.random.nextInt(16) - 8;
            int z = origin.getZ() + world.random.nextInt(16) - 8;
            int y = origin.getY() + world.random.nextInt(4) - 2;
            mutable.set(x, y, z);

            if (world.getLightLevel(LightType.BLOCK, mutable) < cfg.fearLightThreshold) {
                if (world.getBlockState(mutable).isAir()) {
                    return Vec3d.ofBottomCenter(mutable);
                }
            }
        }
        return null;
    }

    @Override
    public void start() {
        if (targetPos != null) {
            mob.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, speed);
        }
    }

    @Override
    public boolean shouldContinue() {
        var cfg = DarknessConfigs.get().server;
        if (mob.getNavigation().isIdle()) return false;

        int blockLight = mob.getWorld()
                .getLightLevel(LightType.BLOCK, mob.getBlockPos());

        return blockLight >= cfg.fearLightThreshold;
    }

}
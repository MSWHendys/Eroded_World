package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.EnumSet;

public final class StartleFromLightGoal extends Goal {

    private final HostileEntity mob;

    private BlockPos lightPos;
    private int ticksLeft;

    private final int escapeDistance;
    private final double speed;

    public StartleFromLightGoal(HostileEntity mob, int escapeDistance, double speed) {
        this.mob = mob;
        this.escapeDistance = escapeDistance;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!mob.isAlive()) return false;
        if (!(mob.getWorld() instanceof ServerWorld world)) return false;

        if (!DarknessEnvironment.isNight(world)) return false;

        BlockPos pos = mob.getBlockPos();

        if (DarknessEnvironment.isDarkForMobs(world, pos)) return false;
        if (mob.getTarget() == null) return false;

        int blockLight = world.getLightLevel(LightType.BLOCK, pos);
        if (blockLight < DarknessConfigs.get().server.fearLightThreshold) return false;

        BlockPos light = DarknessLightResolver.findNearbyBlockLight(world, pos);
        if (light == null) return false;

        this.lightPos = light;
        return true;
    }

    @Override
    public void start() {
        ticksLeft = 20;
        mob.setTarget(null);
    }

    @Override
    public boolean shouldContinue() {
        return mob.isAlive() && ticksLeft-- > 0;
    }

    @Override
    public void stop() {
        lightPos = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(mob.getWorld() instanceof ServerWorld world)) return;
        if (lightPos == null) return;

        mob.setTarget(null);

        BlockPos pos = mob.getBlockPos();
        Vec3d escape = DarknessLightResolver.escapeFrom(pos, lightPos);
        if (escape.lengthSquared() < 0.0001) return;

        BlockPos target = pos.add(
                (int) Math.round(escape.x * escapeDistance),
                0,
                (int) Math.round(escape.z * escapeDistance)
        );

        mob.getNavigation().startMovingTo(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                speed
        );
    }
}

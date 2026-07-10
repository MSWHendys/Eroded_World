package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public final class StartleFromLightGoal extends Goal {

    private final Monster mob;

    private BlockPos lightPos;
    private int ticksLeft;

    private final int escapeDistance;
    private final double speed;

    public StartleFromLightGoal(Monster mob, int escapeDistance, double speed) {
        this.mob = mob;
        this.escapeDistance = escapeDistance;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!mob.isAlive()) return false;
        if (!(mob.level() instanceof ServerLevel world)) return false;

        if (!DarknessEnvironment.isNight(world)) return false;

        BlockPos pos = mob.blockPosition();

        if (DarknessEnvironment.isDarkForMobs(world, pos)) return false;
        if (mob.getTarget() == null) return false;

        int blockLight = world.getBrightness(LightLayer.BLOCK, pos);
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
    public boolean canContinueToUse() {
        return mob.isAlive() && ticksLeft-- > 0;
    }

    @Override
    public void stop() {
        lightPos = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel world)) return;
        if (lightPos == null) return;

        mob.setTarget(null);

        BlockPos pos = mob.blockPosition();
        Vec3 escape = DarknessLightResolver.escapeFrom(pos, lightPos);
        if (escape.lengthSqr() < 0.0001) return;

        BlockPos target = pos.offset(
                (int) Math.round(escape.x * escapeDistance),
                0,
                (int) Math.round(escape.z * escapeDistance)
        );

        mob.getNavigation().moveTo(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                speed
        );
    }
}
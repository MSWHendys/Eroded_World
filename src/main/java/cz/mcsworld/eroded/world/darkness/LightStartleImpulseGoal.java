package cz.mcsworld.eroded.world.darkness;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.EnumSet;

public final class LightStartleImpulseGoal extends Goal {

    private final HostileEntity mob;

    private int ticksLeft = 0;
    private int cooldown = 0;
    private BlockPos lastLight = null;

    public LightStartleImpulseGoal(HostileEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!mob.isAlive()) return false;
        if (!(mob.getWorld() instanceof ServerWorld world)) return false;

        if (cooldown-- > 0) return false;

        BlockPos pos = mob.getBlockPos();

        if (!DarknessEnvironment.isDarkForMobs(world, pos)) return false;

        int blockLight = world.getLightLevel(LightType.BLOCK, pos);
        if (blockLight < DarknessLightResolver.FEAR_LIGHT_THRESHOLD) return false;

        BlockPos light = DarknessLightResolver.findNearbyBlockLight(world, pos);
        if (light == null || light.equals(lastLight)) return false;

        lastLight = light;
        return true;
    }

    @Override
    public void start() {
        ticksLeft = 6;
        cooldown = 40;
        mob.setTarget(null);
    }

    @Override
    public boolean shouldContinue() {
        return ticksLeft-- > 0;
    }

    @Override
    public void tick() {
        if (!(mob.getWorld() instanceof ServerWorld world)) return;

        BlockPos pos = mob.getBlockPos();
        BlockPos light = DarknessLightResolver.findNearbyBlockLight(world, pos);
        if (light == null) return;

        Vec3d escape = DarknessLightResolver.escapeFrom(pos, light);
        if (escape.lengthSquared() < 0.0001) return;

        mob.addVelocity(escape.x * 0.35, 0.05, escape.z * 0.35);
        mob.velocityDirty = true;
        mob.setTarget(null);
    }
}

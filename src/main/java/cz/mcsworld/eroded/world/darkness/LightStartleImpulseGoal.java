package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public final class LightStartleImpulseGoal extends Goal {

    private final Monster mob;

    private int ticksLeft = 0;
    private int cooldown = 0;
    private BlockPos lastLight = null;

    public LightStartleImpulseGoal(Monster mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        var cfg = DarknessConfigs.get().server;
        if (!mob.isAlive()) return false;
        if (!(mob.level() instanceof ServerLevel world)) return false;

        if (cooldown-- > 0) return false;

        BlockPos pos = mob.blockPosition();

        if (!DarknessEnvironment.isDarkForMobs(world, pos)) return false;

        int blockLight = world.getBrightness(LightLayer.BLOCK, pos);
        if (blockLight < cfg.fearLightThreshold) return false;

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
    public boolean canContinueToUse() {
        return ticksLeft-- > 0;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel world)) return;

        BlockPos pos = mob.blockPosition();
        BlockPos light = DarknessLightResolver.findNearbyBlockLight(world, pos);
        if (light == null) return;

        Vec3 escape = DarknessLightResolver.escapeFrom(pos, light);
        if (escape.lengthSqr() < 0.0001) return;

        mob.push(escape.x * 0.35, 0.05, escape.z * 0.35);
        mob.hasImpulse = true;
        mob.setTarget(null);
    }
}
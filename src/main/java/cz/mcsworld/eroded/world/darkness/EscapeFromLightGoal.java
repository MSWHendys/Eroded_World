package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public final class EscapeFromLightGoal extends Goal {
    private final Monster mob;
    private Vec3 targetPos;

    public EscapeFromLightGoal(Monster mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {

        var root = DarknessConfigs.get();
        var cfg = root.server;
        if (!root.enabled || !cfg.mobLightFearEnabled) return false;
        if (!mob.isAlive() || mob.getTarget() != null) return false;

        ServerLevel world = (ServerLevel) mob.level();
        BlockPos pos = mob.blockPosition();
        if (!DarknessEnvironment.isDarkForMobs(world, pos)) return false;
        if (world.getBrightness(LightLayer.BLOCK, pos) < cfg.fearLightThreshold) return false;

        this.targetPos = findDarkPlace(world, pos);
        return this.targetPos != null;
    }

    private Vec3 findDarkPlace(ServerLevel world, BlockPos origin) {
        var cfg = DarknessConfigs.get().server;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 15; i++) {
            int x = origin.getX() + world.getRandom().nextInt(16) - 8;
            int z = origin.getZ() + world.getRandom().nextInt(16) - 8;
            int y = origin.getY() + world.getRandom().nextInt(4) - 2;
            mutable.set(x, y, z);

            if (world.getBrightness(LightLayer.BLOCK, mutable) < cfg.fearLightThreshold) {
                if (world.getBlockState(mutable).isAir()) {
                    return Vec3.atBottomCenterOf(mutable);
                }
            }
        }
        return null;
    }

    @Override
    public void start() {
        if (targetPos != null) {
            mob.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, DarknessConfigs.get().server.escapeSpeed);
        }
    }

    @Override
    public boolean canContinueToUse() {
        var root = DarknessConfigs.get();
        var cfg = root.server;
        if (!root.enabled || !cfg.mobLightFearEnabled) return false;
        if (mob.getNavigation().isDone()) return false;

        int blockLight = mob.level()
                .getBrightness(LightLayer.BLOCK, mob.blockPosition());

        return blockLight >= cfg.fearLightThreshold;
    }

}
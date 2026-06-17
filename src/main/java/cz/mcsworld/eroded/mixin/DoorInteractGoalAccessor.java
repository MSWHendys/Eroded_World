package cz.mcsworld.eroded.mixin;

import net.minecraft.entity.ai.goal.DoorInteractGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DoorInteractGoal.class)
public interface DoorInteractGoalAccessor {

    @Accessor("doorPos")
    BlockPos eroded$getDoorPos();

    @Accessor("mob")
    MobEntity eroded$getMob();
}
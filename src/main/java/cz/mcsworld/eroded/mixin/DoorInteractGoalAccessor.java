package cz.mcsworld.eroded.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DoorInteractGoal.class)
public interface DoorInteractGoalAccessor {

    @Accessor("doorPos")
    BlockPos eroded$getDoorPos();

    @Accessor("mob")
    Mob eroded$getMob();
}
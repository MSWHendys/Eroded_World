package cz.mcsworld.eroded.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FarmBlock.class)
public interface FarmlandBlockInvoker {

    @Invoker("turnToDirt")
    static void eroded$setToDirt(
            Entity entity,
            BlockState state,
            Level world,
            BlockPos pos
    ) {
        throw new AssertionError();
    }
}
package cz.mcsworld.eroded.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FarmlandBlock.class)
public interface FarmlandBlockInvoker {

    @Invoker("setToDirt")
    static void eroded$setToDirt(
            Entity entity,
            BlockState state,
            World world,
            BlockPos pos
    ) {
        throw new AssertionError();
    }
}
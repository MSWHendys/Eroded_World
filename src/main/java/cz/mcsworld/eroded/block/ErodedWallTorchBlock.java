package cz.mcsworld.eroded.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class ErodedWallTorchBlock extends WallTorchBlock {

    private static final float SMOKE_CHANCE = 0.08F;

    public ErodedWallTorchBlock(BlockBehaviour.Properties settings) {
        super(ParticleTypes.SMOKE, settings);
    }

    @Override
    public void animateTick(
            @NotNull BlockState state,
            @NotNull Level world,
            @NotNull BlockPos pos,
            RandomSource random
    ) {
        if (random.nextFloat() > SMOKE_CHANCE) {
            return;
        }

        Direction direction = state.getValue(FACING);

        double x = pos.getX() + 0.5D - direction.getStepX() * 0.27D;
        double y = pos.getY() + 0.72D;
        double z = pos.getZ() + 0.5D - direction.getStepZ() * 0.27D;

        world.addParticle(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                0.0D,
                0.015D,
                0.0D
        );
    }
}
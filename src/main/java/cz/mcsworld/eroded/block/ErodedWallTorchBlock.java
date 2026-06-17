package cz.mcsworld.eroded.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public final class ErodedWallTorchBlock extends WallTorchBlock {

    private static final float SMOKE_CHANCE = 0.08F;

    public ErodedWallTorchBlock(AbstractBlock.Settings settings) {
        super(ParticleTypes.SMOKE, settings);
    }

    @Override
    public void randomDisplayTick(
            BlockState state,
            World world,
            BlockPos pos,
            Random random
    ) {
        if (random.nextFloat() > SMOKE_CHANCE) {
            return;
        }

        Direction direction = state.get(FACING);

        double x = pos.getX() + 0.5D - direction.getOffsetX() * 0.27D;
        double y = pos.getY() + 0.72D;
        double z = pos.getZ() + 0.5D - direction.getOffsetZ() * 0.27D;

        world.addParticleClient(
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
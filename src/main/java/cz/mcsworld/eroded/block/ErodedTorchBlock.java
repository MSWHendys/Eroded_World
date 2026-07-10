package cz.mcsworld.eroded.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class ErodedTorchBlock extends TorchBlock {

    private static final float SMOKE_CHANCE = 0.08F;

    public ErodedTorchBlock(BlockBehaviour.Properties settings) {
        super(ParticleTypes.SMOKE, settings);
    }

    @Override
    public void animateTick(
            BlockState state,
            Level world,
            BlockPos pos,
            RandomSource random
    ) {
        if (random.nextFloat() > SMOKE_CHANCE) {
            return;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.72D;
        double z = pos.getZ() + 0.5D;

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
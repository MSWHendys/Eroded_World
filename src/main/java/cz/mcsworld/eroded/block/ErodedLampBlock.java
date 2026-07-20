package cz.mcsworld.eroded.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ErodedLampBlock extends LanternBlock {
    public ErodedLampBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClientSide && world instanceof ServerLevel serverWorld) {

            world.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0f, 1.2f);

            AABB area = new AABB(pos).inflate(8.0);
            world.getEntitiesOfClass(Monster.class, area, mob -> true).forEach(mob -> {
                serverWorld.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 10, 0.2, 0.5, 0.2, 0.05);
                mob.discard();
            });

            serverWorld.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);

            world.destroyBlock(pos, false);
        }
    }
}
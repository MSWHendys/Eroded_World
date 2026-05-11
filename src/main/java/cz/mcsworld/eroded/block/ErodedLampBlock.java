package cz.mcsworld.eroded.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.LanternBlock;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

public class ErodedLampBlock extends LanternBlock {
    public ErodedLampBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {

            world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0f, 1.2f);

            Box area = new Box(pos).expand(8.0);
            world.getEntitiesByClass(HostileEntity.class, area, mob -> true).forEach(mob -> {
                serverWorld.spawnParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 10, 0.2, 0.5, 0.2, 0.05);
                mob.discard();
            });

            serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);

            world.breakBlock(pos, false);
        }
    }
}
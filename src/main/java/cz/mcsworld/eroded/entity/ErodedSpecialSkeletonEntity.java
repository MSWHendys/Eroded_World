package cz.mcsworld.eroded.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ErodedSpecialSkeletonEntity extends SkeletonEntity {

    public ErodedSpecialSkeletonEntity(EntityType<? extends SkeletonEntity> type, World world) {
        super(type, world);
    }

    @Override
    public boolean isAffectedByDaylight() {
        return false;
    }

    @Override
    public EntityData initialize(
            ServerWorldAccess world,
            LocalDifficulty difficulty,
            SpawnReason spawnReason,
            @Nullable EntityData entityData
    ) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);

        ErodedMobSunBehaviour.applyRandomSunBehaviour(this, getRandom());

        return data;
    }

    @Override
    public void tick() {
        super.tick();

        if (getWorld().isClient()) {
            return;
        }

        ErodedMobSunBehaviour.tickSunBehaviour(this, true);
    }
}
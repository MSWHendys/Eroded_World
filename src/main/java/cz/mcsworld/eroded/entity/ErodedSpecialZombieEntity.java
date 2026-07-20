package cz.mcsworld.eroded.entity;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class ErodedSpecialZombieEntity extends Zombie {

    public ErodedSpecialZombieEntity(EntityType<? extends Zombie> type, Level world) {
        super(type, world);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor world,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData entityData
    ) {
        SpawnGroupData data = super.finalizeSpawn(world, difficulty, spawnReason, entityData);
        ErodedMobSunBehaviour.applyRandomSunBehaviour(this, getRandom());

        return data;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            return;
        }

        ErodedMobSunBehaviour.tickSunBehaviour(this, false);
    }
}
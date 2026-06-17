package cz.mcsworld.eroded.core;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.entity.ErodedSpecialSkeletonEntity;
import cz.mcsworld.eroded.entity.ErodedSpecialZombieEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ErodedEntities {

    public static final EntityType<ErodedSpecialSkeletonEntity> ERODED_SPECIAL_SKELETON = register(
            "eroded_special_skeleton",
            EntityType.Builder.create(ErodedSpecialSkeletonEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6F, 1.99F)
                    .eyeHeight(1.74F)
                    .maxTrackingRange(8)
    );

    public static final EntityType<ErodedSpecialZombieEntity> ERODED_SPECIAL_ZOMBIE = register(
            "eroded_special_zombie",
            EntityType.Builder.create(ErodedSpecialZombieEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .maxTrackingRange(8)
    );

    private ErodedEntities() {
    }

    public static void register() {

    }

    private static <T extends Entity> EntityType<T> register(
            String name,
            EntityType.Builder<T> builder
    ) {
        Identifier id = Identifier.of(ErodedMod.MOD_ID, name);
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id);

        return Registry.register(
                Registries.ENTITY_TYPE,
                key,
                builder.build(key)
        );
    }
}
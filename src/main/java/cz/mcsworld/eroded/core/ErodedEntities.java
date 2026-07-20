package cz.mcsworld.eroded.core;

import cz.mcsworld.eroded.ErodedMod;
import cz.mcsworld.eroded.entity.ErodedSpecialSkeletonEntity;
import cz.mcsworld.eroded.entity.ErodedSpecialZombieEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.NotNull;

public final class ErodedEntities {

    public static final EntityType<@NotNull ErodedSpecialSkeletonEntity> ERODED_SPECIAL_SKELETON = register(
            "eroded_special_skeleton",
            EntityType.Builder.of(ErodedSpecialSkeletonEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(8)
    );

    public static final EntityType<@NotNull ErodedSpecialZombieEntity> ERODED_SPECIAL_ZOMBIE = register(
            "eroded_special_zombie",
            EntityType.Builder.of(ErodedSpecialZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(8)
    );

    private ErodedEntities() {
    }

    public static void register() {

    }

    private static <T extends Entity> EntityType<@NotNull T> register(
            String name,
            EntityType.Builder<@NotNull T> builder
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, name);
        ResourceKey<@NotNull EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);

        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                key,
                builder.build(key)
        );
    }
}
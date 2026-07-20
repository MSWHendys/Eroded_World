package cz.mcsworld.eroded.core;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.NotNull;

public final class ErodedEntityItems {

    public static Item ERODED_SPECIAL_SKELETON_SPAWN_EGG;
    public static Item ERODED_SPECIAL_ZOMBIE_SPAWN_EGG;

    private ErodedEntityItems() {
    }

  public static void register() {
        ERODED_SPECIAL_SKELETON_SPAWN_EGG = registerSpawnEgg(
                "eroded_special_skeleton_spawn_egg",
                ErodedEntities.ERODED_SPECIAL_SKELETON
        );

        ERODED_SPECIAL_ZOMBIE_SPAWN_EGG = registerSpawnEgg(
                "eroded_special_zombie_spawn_egg",
                ErodedEntities.ERODED_SPECIAL_ZOMBIE
        );
    }

    private static Item registerSpawnEgg(
            String name,
            EntityType<? extends @NotNull Mob> entityType
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, name);

        ResourceKey<@NotNull Item> key = ResourceKey.create(
                Registries.ITEM,
                id
        );

        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new SpawnEggItem(
                        new Item.Properties()
                                .setId(key)
                                .spawnEgg(entityType)
                )
        );
    }
}
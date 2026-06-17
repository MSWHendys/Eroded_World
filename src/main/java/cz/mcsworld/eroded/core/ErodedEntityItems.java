package cz.mcsworld.eroded.core;

import cz.mcsworld.eroded.ErodedMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

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
            EntityType<? extends MobEntity> entityType
    ) {
        Identifier id = Identifier.of(ErodedMod.MOD_ID, name);

        RegistryKey<Item> key = RegistryKey.of(
                RegistryKeys.ITEM,
                id
        );

        return Registry.register(
                Registries.ITEM,
                key,
                new SpawnEggItem(
                        entityType,
                        new Item.Settings().registryKey(key)
                )
        );
    }
}
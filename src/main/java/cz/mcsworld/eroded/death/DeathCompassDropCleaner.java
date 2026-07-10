package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.core.ErodedItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import java.util.List;

public final class DeathCompassDropCleaner {

    private DeathCompassDropCleaner() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(
                DeathCompassDropCleaner::afterDeath
        );
    }

    private static void afterDeath(
            LivingEntity entity,
            net.minecraft.world.damagesource.DamageSource source
    ) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel world)) return;

        double radius =
                DeathConfig.get().compass.heartbeat.dropCleanupRadius;

        AABB box = player.getBoundingBox().inflate(radius);

        List<ItemEntity> items =
                world.getEntitiesOfClass(
                        ItemEntity.class,
                        box,
                        e -> e.getItem().is(ErodedItems.DEATH_COMPASS)
                );

        for (ItemEntity item : items) {
            item.discard();
        }
    }
}

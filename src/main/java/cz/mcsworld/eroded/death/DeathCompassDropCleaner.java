package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.core.ErodedItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

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
            net.minecraft.entity.damage.DamageSource source
    ) {
        if (!(entity instanceof ServerPlayerEntity player)) return;
        if (!(player.getWorld() instanceof ServerWorld world)) return;

        double radius =
                DeathConfig.get().compass.heartbeat.dropCleanupRadius;

        Box box = player.getBoundingBox().expand(radius);

        List<ItemEntity> items =
                world.getEntitiesByClass(
                        ItemEntity.class,
                        box,
                        e -> e.getStack().isOf(ErodedItems.DEATH_COMPASS)
                );

        for (ItemEntity item : items) {
            item.discard();
        }
    }
}

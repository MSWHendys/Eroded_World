package cz.mcsworld.eroded.death;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

public final class DeathHologramInteractBlocker {

    private DeathHologramInteractBlocker() {}

    public static void register() {
        UseEntityCallback.EVENT.register(
                (player, world, hand, entity, hitResult) -> {

                    if (world.isClientSide()) return InteractionResult.PASS;

                    if (isDeathHologram(entity)) {
                        return InteractionResult.FAIL;
                    }

                    return InteractionResult.PASS;
                }
        );
    }

    private static boolean isDeathHologram(Entity entity) {
        return entity.getTags()
                .contains("eroded_death_hologram");
    }
}

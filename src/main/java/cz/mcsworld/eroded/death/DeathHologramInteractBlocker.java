package cz.mcsworld.eroded.death;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;

public final class DeathHologramInteractBlocker {

    private DeathHologramInteractBlocker() {}

    public static void register() {
        UseEntityCallback.EVENT.register(
                (player, world, hand, entity, hitResult) -> {

                    if (world.isClient()) return ActionResult.PASS;

                    if (isDeathHologram(entity)) {
                        return ActionResult.FAIL;
                    }

                    return ActionResult.PASS;
                }
        );
    }

    private static boolean isDeathHologram(Entity entity) {
        return entity.getCommandTags()
                .contains("eroded_death_hologram");
    }
}

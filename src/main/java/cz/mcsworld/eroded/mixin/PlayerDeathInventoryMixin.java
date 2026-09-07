package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.death.DeathChestHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the inventory at vanilla's real inventory-drop phase instead of
 * guessing a lethal hit from raw incoming damage.
 */
@Mixin(Player.class)
public abstract class PlayerDeathInventoryMixin {

    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void eroded$captureCommittedDeathInventory(
            ServerLevel world,
            CallbackInfo ci
    ) {
        if (!((Object) this instanceof ServerPlayer player)) {
            return;
        }

        if (DeathChestHandler.captureDeathInventory(player)) {
            ci.cancel();
        }
    }
}

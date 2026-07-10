package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.death.ErodedPortalMemoryState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class EntityPortalMixin {

    @Inject(method = "teleport", at = @At("HEAD"))
    private void onPortalTeleport(
            TeleportTransition target,
            CallbackInfoReturnable<Entity> cir
    ) {

        ServerPlayer player = (ServerPlayer) (Object) this;

        ServerLevel origin = player.level();
        ServerLevel destination = target.newLevel();

        if (origin.dimension().equals(Level.OVERWORLD)
                && destination.dimension().equals(Level.NETHER)) {

            BlockPos approx = BlockPos.containing(
                    player.getX(),
                    player.getY(),
                    player.getZ()
            );

            BlockPos portal =
                    findNearestPortal(origin, approx, 4);

            if (portal != null) {
                ErodedPortalMemoryState
                        .get(origin)
                        .setOverworldPortal(player.getUUID(), portal);

            }
        }
    }
    private static BlockPos findNearestPortal(ServerLevel world, BlockPos center, int radius) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    m.set(
                            center.getX() + x,
                            center.getY() + y,
                            center.getZ() + z
                    );

                    if (world.getBlockState(m).is(net.minecraft.world.level.block.Blocks.NETHER_PORTAL)) {
                        return m.immutable();
                    }
                }
            }
        }
        return null;
    }
}
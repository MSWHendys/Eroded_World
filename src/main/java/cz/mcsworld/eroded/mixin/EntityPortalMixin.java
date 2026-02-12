package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.death.ErodedPortalMemoryState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class EntityPortalMixin {

    @Inject(method = "teleportTo", at = @At("HEAD"))
    private void onPortalTeleport(
            TeleportTarget target,
            CallbackInfoReturnable<Entity> cir
    ) {

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        ServerWorld origin = player.getWorld();
        ServerWorld destination = target.world();

        if (origin.getRegistryKey().equals(World.OVERWORLD)
                && destination.getRegistryKey().equals(World.NETHER)) {

            BlockPos approx = BlockPos.ofFloored(
                    player.getX(),
                    player.getY(),
                    player.getZ()
            );

            BlockPos portal =
                    findNearestPortal(origin, approx, 4);

            if (portal != null) {
                ErodedPortalMemoryState
                        .get(origin)
                        .setOverworldPortal(player.getUuid(), portal);

            }
        }
    }
    private static BlockPos findNearestPortal(ServerWorld world, BlockPos center, int radius) {
        BlockPos.Mutable m = new BlockPos.Mutable();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    m.set(
                            center.getX() + x,
                            center.getY() + y,
                            center.getZ() + z
                    );

                    if (world.getBlockState(m).isOf(net.minecraft.block.Blocks.NETHER_PORTAL)) {
                        return m.toImmutable();
                    }
                }
            }
        }
        return null;
    }
}
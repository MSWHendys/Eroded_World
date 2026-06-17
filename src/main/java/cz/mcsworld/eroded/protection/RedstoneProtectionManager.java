package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class RedstoneProtectionManager {

    private RedstoneProtectionManager() {
    }

    public static boolean canPlayerUseControl(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {

        if (ExplosionProtectionManager.isProtected(world, pos)
                && ExplosionProtectionManager.preventRedstoneControls()
                && !ExplosionProtectionManager.hasBypassAccess(player)) {
            return false;
        }

        return TerritoryProtectionManager.canUseRedstone(player, world, pos);
    }

    public static boolean canEntityTriggerPressurePlate(
            Entity entity,
            ServerWorld world,
            BlockPos pos
    ) {

        if (ExplosionProtectionManager.isProtected(world, pos)
                && ExplosionProtectionManager.preventPressurePlates()) {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return false;
            }

            if (!ExplosionProtectionManager.hasBypassAccess(player)) {
                return false;
            }
        }

        if (TerritoryProtectionManager.getActiveClaimAt(world, pos) != null) {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return false;
            }

            return TerritoryProtectionManager.canUseRedstone(player, world, pos);
        }

        return true;
    }
}
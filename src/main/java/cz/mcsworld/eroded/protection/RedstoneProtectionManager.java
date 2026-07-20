package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class RedstoneProtectionManager {

    private RedstoneProtectionManager() {
    }

    public static boolean canPlayerUseControl(
            ServerPlayer player,
            ServerLevel world,
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
            ServerLevel world,
            BlockPos pos
    ) {

        if (ExplosionProtectionManager.isProtected(world, pos)
                && ExplosionProtectionManager.preventPressurePlates()) {
            if (!(entity instanceof ServerPlayer player)) {
                return false;
            }

            if (!ExplosionProtectionManager.hasBypassAccess(player)) {
                return false;
            }
        }

        if (TerritoryProtectionManager.getActiveClaimAt(world, pos) != null) {
            if (!(entity instanceof ServerPlayer player)) {
                return false;
            }

            return TerritoryProtectionManager.canUseRedstone(player, world, pos);
        }

        return true;
    }
}
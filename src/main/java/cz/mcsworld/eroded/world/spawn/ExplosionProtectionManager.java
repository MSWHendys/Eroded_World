package cz.mcsworld.eroded.world.spawn;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class ExplosionProtectionManager {

    private ExplosionProtectionManager() {
    }

    private static TerritoryConfig.Server cfg() {
        return TerritoryConfig.get().server;
    }

    public static boolean isSpawnProtectionEnabled() {
        return cfg().enabled && cfg().spawnProtectionEnabled;
    }

    public static boolean isExplosionProtectionEnabled() {
        return isSpawnProtectionEnabled() && cfg().preventExplosions;
    }

    public static boolean isProtected(ServerWorld world, BlockPos pos) {
        if (!isSpawnProtectionEnabled()) {
            return false;
        }

        BlockPos spawn = world.getSpawnPos();

        int radius = cfg().spawnProtectionRadius;

        int dx = Math.abs(pos.getX() - spawn.getX());
        int dz = Math.abs(pos.getZ() - spawn.getZ());

        return dx <= radius && dz <= radius;
    }

    public static boolean canBreak(ServerPlayerEntity player, BlockPos pos) {
        if (hasBypass(player)) {
            return true;
        }

        if (!isSpawnProtectionEnabled() || !cfg().preventBlockBreak) {
            return true;
        }

        return !isProtected(player.getWorld(), pos);
    }

    public static boolean canPlace(ServerPlayerEntity player, BlockPos pos) {
        if (hasBypass(player)) {
            return true;
        }

        if (!isSpawnProtectionEnabled() || !cfg().preventBlockPlace) {
            return true;
        }

        return !isProtected(player.getWorld(), pos);
    }

    public static boolean canUseSpecialBlock(ServerPlayerEntity player, BlockPos pos) {
        if (hasBypass(player)) {
            return true;
        }

        if (!preventSpecialBlockUse()) {
            return true;
        }

        return !isProtected(player.getWorld(), pos);
    }

    public static boolean canUseContainer(ServerPlayerEntity player, BlockPos pos) {
        if (hasBypass(player)) {
            return true;
        }

        if (!isSpawnProtectionEnabled() || !cfg().preventContainerUse) {
            return true;
        }

        return !isProtected(player.getWorld(), pos);
    }

    public static boolean preventPistonPush() {
        return isSpawnProtectionEnabled() && cfg().preventPistonPush;
    }

    public static boolean preventRedstoneControls() {
        return isSpawnProtectionEnabled() && cfg().preventRedstoneControls;
    }

    public static boolean preventPressurePlates() {
        return isSpawnProtectionEnabled() && cfg().preventPressurePlates;
    }

    public static boolean preventProtectedEntityInteraction() {
        return isSpawnProtectionEnabled() && cfg().preventProtectedEntityInteraction;
    }

    public static boolean preventFluidFlow() {
        return isSpawnProtectionEnabled() && cfg().preventFluidFlow;
    }

    public static boolean preventInventoryAutomationTransfer() {
        return isSpawnProtectionEnabled() && cfg().preventInventoryAutomationTransfer;
    }

    public static boolean preventDispenserDropperBoundaryActions() {
        return isSpawnProtectionEnabled() && cfg().preventDispenserDropperBoundaryActions;
    }

    public static boolean preventProjectileBoundaryActions() {
        return isSpawnProtectionEnabled() && cfg().spawnPreventProjectileBoundaryActions;
    }

    public static boolean preventMobGriefing() {
        return isSpawnProtectionEnabled() && cfg().spawnPreventMobGriefing;
    }

    public static boolean preventVehicles() {
        return isSpawnProtectionEnabled() && cfg().spawnPreventVehicles;
    }

    public static boolean preventSpecialBlockUse() {
        return isSpawnProtectionEnabled() && cfg().spawnPreventSpecialBlockUse;
    }

    public static boolean hasBypassAccess(ServerPlayerEntity player) {
        return hasBypass(player);
    }

    private static boolean hasBypass(ServerPlayerEntity player) {
        if (cfg().bypassCreative && player.isCreative()) {
            return true;
        }

        return cfg().bypassOP && player.hasPermissionLevel(2);
    }
}
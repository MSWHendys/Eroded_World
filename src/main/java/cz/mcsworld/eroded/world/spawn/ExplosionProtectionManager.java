package cz.mcsworld.eroded.world.spawn;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

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

    public static boolean isProtected(ServerLevel world, BlockPos pos) {
        if (!isSpawnProtectionEnabled()) {
            return false;
        }

        BlockPos spawn = world.getRespawnData().pos();

        int radius = cfg().spawnProtectionRadius;

        int dx = Math.abs(pos.getX() - spawn.getX());
        int dz = Math.abs(pos.getZ() - spawn.getZ());

        return dx <= radius && dz <= radius;
    }

    public static boolean canBreak(ServerPlayer player, BlockPos pos) {
        if (hasBypass(player)) {
            return true;
        }

        if (!isSpawnProtectionEnabled() || !cfg().preventBlockBreak) {
            return true;
        }

        return !isProtected(player.level(), pos);
    }

    public static boolean canPlace(ServerPlayer player, BlockPos pos) {
        if (hasBypass(player)) {
            return true;
        }

        if (!isSpawnProtectionEnabled() || !cfg().preventBlockPlace) {
            return true;
        }

        return !isProtected(player.level(), pos);
    }

    public static boolean canUseSpecialBlock(ServerPlayer player, BlockPos pos) {
        if (hasBypass(player)) {
            return true;
        }

        if (!preventSpecialBlockUse()) {
            return true;
        }

        return !isProtected(player.level(), pos);
    }

    public static boolean canUseContainer(ServerPlayer player, BlockPos pos) {
        if (hasBypass(player)) {
            return true;
        }

        if (!isSpawnProtectionEnabled() || !cfg().preventContainerUse) {
            return true;
        }

        return !isProtected(player.level(), pos);
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

    public static boolean hasBypassAccess(ServerPlayer player) {
        return hasBypass(player);
    }

    private static boolean hasBypass(ServerPlayer player) {
        if (cfg().bypassCreative && player.isCreative()) {
            return true;
        }

        return cfg().bypassOP && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
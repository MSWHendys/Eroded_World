package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.VehicleEntity;

public final class VehicleProtectionManager {

    private VehicleProtectionManager() {
    }

    private static TerritoryConfig.Server cfg() {
        return TerritoryConfig.get().server;
    }

    private static boolean isClaimVehicleProtectionEnabled() {
        return cfg().enabled
                && cfg().playerClaimProtectionEnabled
                && cfg().protectClaimVehicles;
    }

    private static boolean isSpawnVehicleProtectionEnabled() {
        return ExplosionProtectionManager.preventVehicles();
    }

    public static boolean isProtectedVehicle(Entity entity) {
        return entity instanceof VehicleEntity;
    }

    public static boolean isStorageVehicle(Entity entity) {
        return entity instanceof VehicleEntity
                && entity instanceof Container;
    }

    private static boolean isInClaim(ServerLevel world, BlockPos pos) {
        return TerritoryProtectionManager.getAnchorClaim(world, pos) != null
                || TerritoryProtectionManager.getActiveClaimAt(world, pos) != null;
    }

    public static boolean canUseVehicle(
            ServerPlayer player,
            ServerLevel world,
            Entity vehicle
    ) {
        if (!isProtectedVehicle(vehicle)) {
            return true;
        }

        BlockPos pos = vehicle.blockPosition();

        if (ExplosionProtectionManager.isProtected(world, pos)) {
            if (isStorageVehicle(vehicle)
                    && !ExplosionProtectionManager.canUseContainer(player, pos)) {
                return false;
            }

            if (!isSpawnVehicleProtectionEnabled()) {
                return true;
            }

            return ExplosionProtectionManager.hasBypassAccess(player);
        }

        if (!isClaimVehicleProtectionEnabled()) {
            return true;
        }

        if (!isInClaim(world, pos)) {
            return true;
        }

        if (isStorageVehicle(vehicle)) {
            return TerritoryProtectionManager.canOpenContainer(
                    player,
                    world,
                    pos
            );
        }

        return TerritoryProtectionManager.canModifyEntity(
                player,
                world,
                pos
        );
    }

    public static boolean canAttackVehicle(
            ServerPlayer player,
            ServerLevel world,
            Entity vehicle
    ) {
        if (!isProtectedVehicle(vehicle)) {
            return true;
        }

        BlockPos pos = vehicle.blockPosition();

        if (ExplosionProtectionManager.isProtected(world, pos)) {
            if (isStorageVehicle(vehicle)
                    && !ExplosionProtectionManager.canUseContainer(player, pos)) {
                return false;
            }

            if (!isSpawnVehicleProtectionEnabled()) {
                return true;
            }

            return ExplosionProtectionManager.hasBypassAccess(player);
        }

        if (!isClaimVehicleProtectionEnabled()) {
            return true;
        }

        if (!isInClaim(world, pos)) {
            return true;
        }

        if (isStorageVehicle(vehicle)) {
            return TerritoryProtectionManager.canModifyEntity(player, world, pos)
                    && TerritoryProtectionManager.canOpenContainer(player, world, pos);
        }

        return TerritoryProtectionManager.canModifyEntity(
                player,
                world,
                pos
        );
    }

    public static boolean canDamageVehicle(
            Entity vehicle,
            ServerLevel world,
            DamageSource source
    ) {
        if (!isProtectedVehicle(vehicle)) {
            return true;
        }

        BlockPos pos = vehicle.blockPosition();

        if (source.getEntity() instanceof ServerPlayer player) {
            return canAttackVehicle(player, world, vehicle);
        }

        if (ExplosionProtectionManager.isProtected(world, pos)) {
            return !isSpawnVehicleProtectionEnabled();
        }

        if (!isClaimVehicleProtectionEnabled()) {
            return true;
        }

        return !isInClaim(world, pos);
    }
}
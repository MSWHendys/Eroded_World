package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

public final class VehicleProtectionEvents {

    private VehicleProtectionEvents() {
    }

    public static void register() {
        registerUseVehicleProtection();
        registerAttackVehicleProtection();
    }

    private static void registerUseVehicleProtection() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            if (!VehicleProtectionManager.isProtectedVehicle(entity)) {
                return ActionResult.PASS;
            }

            if (!VehicleProtectionManager.canUseVehicle(
                    serverPlayer,
                    serverWorld,
                    entity
            )) {
                sendMessageOnlyOutsideSpawn(serverPlayer, serverWorld, entity.getBlockPos());
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    private static void registerAttackVehicleProtection() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            if (!VehicleProtectionManager.isProtectedVehicle(entity)) {
                return ActionResult.PASS;
            }

            if (!VehicleProtectionManager.canAttackVehicle(
                    serverPlayer,
                    serverWorld,
                    entity
            )) {
                sendMessageOnlyOutsideSpawn(serverPlayer, serverWorld, entity.getBlockPos());
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    private static void sendMessageOnlyOutsideSpawn(
            ServerPlayerEntity player,
            ServerWorld world,
            net.minecraft.util.math.BlockPos pos
    ) {
        if (ExplosionProtectionManager.isProtected(world, pos)) {
            return;
        }

        TerritoryProtectionManager.sendProtectedMessage(player);
    }
}
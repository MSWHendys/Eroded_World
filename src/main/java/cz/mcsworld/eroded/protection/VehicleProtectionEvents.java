package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class VehicleProtectionEvents {

    private VehicleProtectionEvents() {
    }

    public static void register() {
        registerUseVehicleProtection();
        registerAttackVehicleProtection();
    }

    private static void registerUseVehicleProtection() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (!(world instanceof ServerLevel serverWorld)) {
                return InteractionResult.PASS;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            if (!VehicleProtectionManager.isProtectedVehicle(entity)) {
                return InteractionResult.PASS;
            }

            if (!VehicleProtectionManager.canUseVehicle(
                    serverPlayer,
                    serverWorld,
                    entity
            )) {
                sendMessageOnlyOutsideSpawn(serverPlayer, serverWorld, entity.blockPosition());
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }

    private static void registerAttackVehicleProtection() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (!(world instanceof ServerLevel serverWorld)) {
                return InteractionResult.PASS;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            if (!VehicleProtectionManager.isProtectedVehicle(entity)) {
                return InteractionResult.PASS;
            }

            if (!VehicleProtectionManager.canAttackVehicle(
                    serverPlayer,
                    serverWorld,
                    entity
            )) {
                sendMessageOnlyOutsideSpawn(serverPlayer, serverWorld, entity.blockPosition());
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }

    private static void sendMessageOnlyOutsideSpawn(
            ServerPlayer player,
            ServerLevel world,
            net.minecraft.core.BlockPos pos
    ) {
        if (ExplosionProtectionManager.isProtected(world, pos)) {
            return;
        }

        TerritoryProtectionManager.sendProtectedMessage(player);
    }
}
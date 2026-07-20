package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;

public final class EntityProtectionEvents {

    private EntityProtectionEvents() {
    }

    public static void register() {
        registerUseEntityProtection();
        registerAttackEntityProtection();
    }

    private static void registerUseEntityProtection() {
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

            if (!isProtectedEntity(entity)) {
                return InteractionResult.PASS;
            }

            BlockPos pos = entity.blockPosition();

            if (!canUseProtectedEntity(serverPlayer, serverWorld, pos)) {
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }

    private static void registerAttackEntityProtection() {
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

            if (!isProtectedEntity(entity)) {
                return InteractionResult.PASS;
            }

            BlockPos pos = entity.blockPosition();

            if (!canAttackProtectedEntity(serverPlayer, serverWorld, pos)) {
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }

    public static boolean isProtectedEntity(Entity entity) {
        return entity instanceof ItemFrame
                || entity instanceof Painting
                || entity instanceof ArmorStand
                || entity instanceof Container;
    }

    private static boolean canUseProtectedEntity(
            ServerPlayer player,
            ServerLevel world,
            BlockPos pos
    ) {

        if (ExplosionProtectionManager.isProtected(world, pos)) {
            if (!ExplosionProtectionManager.preventProtectedEntityInteraction()) {
                return true;
            }

            if (!ExplosionProtectionManager.canPlace(player, pos)) {
                return false;
            }
        }

        return TerritoryProtectionManager.canModifyEntity(player, world, pos);
    }

    private static boolean canAttackProtectedEntity(
            ServerPlayer player,
            ServerLevel world,
            BlockPos pos
    ) {

        if (ExplosionProtectionManager.isProtected(world, pos)) {
            if (!ExplosionProtectionManager.preventProtectedEntityInteraction()) {
                return true;
            }

            if (!ExplosionProtectionManager.canBreak(player, pos)) {
                return false;
            }
        }


        return TerritoryProtectionManager.canModifyEntity(player, world, pos);
    }

    public static boolean canDamageProtectedEntity(
            Entity target,
            ServerLevel world,
            DamageSource source
    ) {
        if (!isProtectedEntity(target)) {
            return true;
        }

        BlockPos pos = target.blockPosition();


        if (source.getEntity() instanceof ServerPlayer player) {
            return canAttackProtectedEntity(player, world, pos);
        }

        if (ExplosionProtectionManager.isProtected(world, pos)
                && ExplosionProtectionManager.preventProtectedEntityInteraction()) {
            return false;
        }


        return TerritoryProtectionManager.getActiveClaimAt(world, pos) == null;
    }
}
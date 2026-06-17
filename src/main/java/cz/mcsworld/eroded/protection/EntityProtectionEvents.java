package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public final class EntityProtectionEvents {

    private EntityProtectionEvents() {
    }

    public static void register() {
        registerUseEntityProtection();
        registerAttackEntityProtection();
    }

    private static void registerUseEntityProtection() {
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

            if (!isProtectedEntity(entity)) {
                return ActionResult.PASS;
            }

            BlockPos pos = entity.getBlockPos();

            if (!canUseProtectedEntity(serverPlayer, serverWorld, pos)) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    private static void registerAttackEntityProtection() {
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

            if (!isProtectedEntity(entity)) {
                return ActionResult.PASS;
            }

            BlockPos pos = entity.getBlockPos();

            if (!canAttackProtectedEntity(serverPlayer, serverWorld, pos)) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    public static boolean isProtectedEntity(Entity entity) {
        return entity instanceof ItemFrameEntity
                || entity instanceof PaintingEntity
                || entity instanceof ArmorStandEntity
                || entity instanceof Inventory;
    }

    private static boolean canUseProtectedEntity(
            ServerPlayerEntity player,
            ServerWorld world,
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
            ServerPlayerEntity player,
            ServerWorld world,
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
            ServerWorld world,
            DamageSource source
    ) {
        if (!isProtectedEntity(target)) {
            return true;
        }

        BlockPos pos = target.getBlockPos();


        if (source.getAttacker() instanceof ServerPlayerEntity player) {
            return canAttackProtectedEntity(player, world, pos);
        }

        if (ExplosionProtectionManager.isProtected(world, pos)
                && ExplosionProtectionManager.preventProtectedEntityInteraction()) {
            return false;
        }


        return TerritoryProtectionManager.getActiveClaimAt(world, pos) == null;
    }
}
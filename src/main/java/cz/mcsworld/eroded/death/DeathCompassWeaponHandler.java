package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.core.ErodedItems;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

public final class DeathCompassWeaponHandler {

    private DeathCompassWeaponHandler() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (!stack.isOf(ErodedItems.DEATH_COMPASS)) {
                return ActionResult.PASS;
            }

            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            DeathConfig cfg = DeathConfig.get();

            if (!cfg.compassWeapon.enabled) {
                return ActionResult.PASS;
            }

            if (cfg.compassWeapon.requireActiveDeathMemory && !hasActiveDeathMemory(serverPlayer)) {
                return ActionResult.PASS;
            }

            if (serverPlayer.getItemCooldownManager().isCoolingDown(stack)) {
                return ActionResult.FAIL;
            }

            if (!canDamageTarget(entity, cfg.compassWeapon)) {
                return ActionResult.FAIL;
            }

            if (!(entity instanceof LivingEntity target)) {
                return ActionResult.PASS;
            }

            boolean damaged = target.damage(
                    serverWorld,
                    serverPlayer.getDamageSources().playerAttack(serverPlayer),
                    cfg.compassWeapon.damage
            );

            if (!damaged) {
                return ActionResult.FAIL;
            }

            serverPlayer.getItemCooldownManager().set(
                    stack,
                    Math.max(1, cfg.compassWeapon.cooldownTicks)
            );

            serverPlayer.swingHand(hand, true);

            return ActionResult.SUCCESS;
        });
    }

    private static boolean canDamageTarget(Entity entity, DeathConfig.CompassWeapon cfg) {
        if (entity instanceof PlayerEntity) {
            return cfg.damagePlayers;
        }

        if (entity instanceof HostileEntity) {
            return cfg.damageHostileMobs;
        }

        if (entity instanceof LivingEntity) {
            return cfg.damageOtherLivingEntities;
        }

        return false;
    }

    private static boolean hasActiveDeathMemory(ServerPlayerEntity player) {
        ErodedDeathMemory mem = ErodedDeathStorage.get(player.getUuid());

        if (mem == null) {
            return false;
        }

        int serverTicks = player.getServer().getTicks();

        return !mem.isExpired(serverTicks) && !mem.isResolved();
    }
}
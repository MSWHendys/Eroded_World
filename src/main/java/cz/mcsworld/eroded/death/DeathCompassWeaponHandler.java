package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.core.ErodedItems;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DeathCompassWeaponHandler {

    private DeathCompassWeaponHandler() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);

            if (!stack.is(ErodedItems.DEATH_COMPASS)) {
                return InteractionResult.PASS;
            }

            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            if (!(world instanceof ServerLevel serverWorld)) {
                return InteractionResult.PASS;
            }

            DeathConfig cfg = DeathConfig.get();

            if (!cfg.compassWeapon.enabled) {
                return InteractionResult.PASS;
            }

            if (cfg.compassWeapon.requireActiveDeathMemory && !hasActiveDeathMemory(serverPlayer)) {
                return InteractionResult.PASS;
            }

            if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.FAIL;
            }

            if (!canDamageTarget(entity, cfg.compassWeapon)) {
                return InteractionResult.FAIL;
            }

            if (!(entity instanceof LivingEntity target)) {
                return InteractionResult.PASS;
            }

            boolean damaged = target.hurtServer(
                    serverWorld,
                    serverPlayer.damageSources().playerAttack(serverPlayer),
                    cfg.compassWeapon.damage
            );

            if (!damaged) {
                return InteractionResult.FAIL;
            }

            serverPlayer.getCooldowns().addCooldown(
                    stack,
                    Math.max(1, cfg.compassWeapon.cooldownTicks)
            );

            serverPlayer.swing(hand, true);

            return InteractionResult.SUCCESS;
        });
    }

    private static boolean canDamageTarget(Entity entity, DeathConfig.CompassWeapon cfg) {
        if (entity instanceof Player) {
            return cfg.damagePlayers;
        }

        if (entity instanceof Monster) {
            return cfg.damageHostileMobs;
        }

        if (entity instanceof LivingEntity) {
            return cfg.damageOtherLivingEntities;
        }

        return false;
    }

    private static boolean hasActiveDeathMemory(ServerPlayer player) {
        ErodedDeathMemory mem = ErodedDeathStorage.get(player.getUUID());

        if (mem == null) {
            return false;
        }

        int serverTicks = player.level().getServer().getTickCount();

        return !mem.isExpired(serverTicks) && !mem.isResolved();
    }
}
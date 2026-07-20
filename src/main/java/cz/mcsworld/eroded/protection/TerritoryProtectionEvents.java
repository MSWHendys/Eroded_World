package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class TerritoryProtectionEvents {

    private TerritoryProtectionEvents() {
    }

    public static void register() {
        registerBlockBreakProtection();
        registerUseBlockProtection();
        registerUseItemProtection();
    }

    private static void registerBlockBreakProtection() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel serverWorld)) {
                return true;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }

            if (!TerritoryProtectionManager.canBreakBlock(serverPlayer, serverWorld, pos)) {
                TerritoryClaim anchorClaim = TerritoryProtectionManager.getAnchorClaim(serverWorld, pos);

                if (anchorClaim != null) {
                    TerritoryProtectionManager.sendAnchorOwnerOnlyMessage(serverPlayer);
                } else {
                    TerritoryProtectionManager.sendProtectedMessage(serverPlayer);
                }

                return false;
            }

            return true;
        });
    }

    private static void registerUseBlockProtection() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (!(world instanceof ServerLevel serverWorld)) {
                return InteractionResult.PASS;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            BlockPos clickedPos = hitResult.getBlockPos();
            BlockPos targetPos = clickedPos.relative(hitResult.getDirection());
            BlockState clickedState = serverWorld.getBlockState(clickedPos);
            ItemStack stack = player.getItemInHand(hand);

            if (isProtectedInteractionBlock(clickedState)) {
                if (!canUseProtectedInteractionBlock(
                        serverPlayer,
                        serverWorld,
                        clickedPos,
                        clickedState
                )) {
                    sendProtectedMessageOnlyOutsideSpawn(
                            serverPlayer,
                            serverWorld,
                            clickedPos
                    );

                    TerritoryProtectionManager.syncInventory(serverPlayer);
                    return InteractionResult.FAIL;
                }
            }

            if (isFireOrFluidItem(stack)) {
                if (!canUseFireOrFluid(serverPlayer, serverWorld, clickedPos, targetPos)) {
                    TerritoryProtectionManager.syncInventory(serverPlayer);
                    return InteractionResult.FAIL;
                }

                return InteractionResult.PASS;
            }

            if (stack.getItem() instanceof BlockItem blockItem
                    && !shouldSkipBlockItemPlacementProtection(clickedState, stack)) {
                if (!ExplosionProtectionManager.canPlace(serverPlayer, targetPos)) {
                    TerritoryProtectionManager.syncInventory(serverPlayer);
                    return InteractionResult.FAIL;
                }

                if (blockItem.getBlock() == ErodedBlocks.TERRITORY_ANCHOR) {
                    if (!TerritoryProtectionManager.validateNewClaim(
                            serverWorld,
                            targetPos,
                            serverPlayer
                    )) {
                        TerritoryProtectionManager.syncInventory(serverPlayer);
                        return InteractionResult.FAIL;
                    }
                }

                if (!TerritoryProtectionManager.canPlaceBlock(serverPlayer, serverWorld, clickedPos)
                        || !TerritoryProtectionManager.canPlaceBlock(serverPlayer, serverWorld, targetPos)) {
                    TerritoryProtectionManager.sendProtectedMessage(serverPlayer);
                    TerritoryProtectionManager.syncInventory(serverPlayer);
                    return InteractionResult.FAIL;
                }

                return InteractionResult.PASS;
            }

            BlockEntity blockEntity = world.getBlockEntity(clickedPos);

            if (blockEntity instanceof Container) {
                if (!ExplosionProtectionManager.canUseContainer(serverPlayer, clickedPos)) {
                    return InteractionResult.FAIL;
                }

                if (!TerritoryProtectionManager.canOpenContainer(serverPlayer, serverWorld, clickedPos)) {
                    TerritoryProtectionManager.sendProtectedMessage(serverPlayer);
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.PASS;
        });
    }

    private static void registerUseItemProtection() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (!(world instanceof ServerLevel serverWorld)) {
                return InteractionResult.PASS;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);

            if (!isBucketItem(stack)) {
                return InteractionResult.PASS;
            }

            HitResult hitResult = player.pick(
                    5.0D,
                    0.0F,
                    stack.is(Items.BUCKET)
            );

            if (!(hitResult instanceof BlockHitResult blockHitResult)) {
                return InteractionResult.PASS;
            }

            if (blockHitResult.getType() == HitResult.Type.MISS) {
                return InteractionResult.PASS;
            }

            BlockPos clickedPos = blockHitResult.getBlockPos();
            BlockPos targetPos = clickedPos.relative(blockHitResult.getDirection());

            if (!canUseFireOrFluid(serverPlayer, serverWorld, clickedPos, targetPos)) {
                TerritoryProtectionManager.syncInventory(serverPlayer);
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }

    private static boolean canUseFireOrFluid(
            ServerPlayer player,
            ServerLevel world,
            BlockPos clickedPos,
            BlockPos targetPos
    ) {

        if (!ExplosionProtectionManager.canPlace(player, clickedPos)
                || !ExplosionProtectionManager.canPlace(player, targetPos)) {
            return false;
        }

        if (!TerritoryProtectionManager.canUseFire(player, world, clickedPos)
                || !TerritoryProtectionManager.canUseFire(player, world, targetPos)) {
            TerritoryProtectionManager.sendProtectedMessage(player);
            return false;
        }

        return true;
    }

    private static boolean isProtectedInteractionBlock(BlockState state) {
        Block block = state.getBlock();

        return block instanceof BedBlock
                || block instanceof RespawnAnchorBlock
                || block instanceof SignBlock
                || block instanceof LecternBlock;
    }

    private static boolean canUseProtectedInteractionBlock(
            ServerPlayer player,
            ServerLevel world,
            BlockPos pos,
            BlockState state
    ) {
        Block block = state.getBlock();

        if (ExplosionProtectionManager.isProtected(world, pos)) {
            return ExplosionProtectionManager.canUseSpecialBlock(player, pos);
        }


        if (block instanceof BedBlock || block instanceof RespawnAnchorBlock) {
            return TerritoryProtectionManager.canUseRedstone(player, world, pos);
        }

        if (block instanceof SignBlock) {
            return TerritoryProtectionManager.canPlaceBlock(player, world, pos);
        }

        if (block instanceof LecternBlock) {
            return TerritoryProtectionManager.canOpenContainer(player, world, pos);
        }

        return true;
    }

    private static void sendProtectedMessageOnlyOutsideSpawn(
            ServerPlayer player,
            ServerLevel world,
            BlockPos pos
    ) {
        if (ExplosionProtectionManager.isProtected(world, pos)) {
            return;
        }

        TerritoryProtectionManager.sendProtectedMessage(player);
    }

    private static boolean shouldSkipBlockItemPlacementProtection(
            BlockState clickedState,
            ItemStack stack
    ) {

        return clickedState.getBlock() instanceof RespawnAnchorBlock
                && stack.is(Items.GLOWSTONE);
    }

    private static boolean isFireOrFluidItem(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL)
                || stack.is(Items.FIRE_CHARGE)
                || isBucketItem(stack);
    }

    private static boolean isBucketItem(ItemStack stack) {
        return stack.is(Items.LAVA_BUCKET)
                || stack.is(Items.WATER_BUCKET)
                || stack.is(Items.BUCKET);
    }
}
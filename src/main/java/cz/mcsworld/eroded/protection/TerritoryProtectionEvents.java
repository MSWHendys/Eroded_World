package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

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
            if (!(world instanceof ServerWorld serverWorld)) {
                return true;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
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
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            BlockPos clickedPos = hitResult.getBlockPos();
            BlockPos targetPos = clickedPos.offset(hitResult.getSide());
            BlockState clickedState = serverWorld.getBlockState(clickedPos);
            ItemStack stack = player.getStackInHand(hand);

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
                    return ActionResult.FAIL;
                }
            }

            if (isFireOrFluidItem(stack)) {
                if (!canUseFireOrFluid(serverPlayer, serverWorld, clickedPos, targetPos)) {
                    TerritoryProtectionManager.syncInventory(serverPlayer);
                    return ActionResult.FAIL;
                }

                return ActionResult.PASS;
            }

            if (stack.getItem() instanceof BlockItem blockItem
                    && !shouldSkipBlockItemPlacementProtection(clickedState, stack)) {
                if (!ExplosionProtectionManager.canPlace(serverPlayer, targetPos)) {
                    TerritoryProtectionManager.syncInventory(serverPlayer);
                    return ActionResult.FAIL;
                }

                if (blockItem.getBlock() == ErodedBlocks.TERRITORY_ANCHOR) {
                    if (!TerritoryProtectionManager.validateNewClaim(
                            serverWorld,
                            targetPos,
                            serverPlayer
                    )) {
                        TerritoryProtectionManager.syncInventory(serverPlayer);
                        return ActionResult.FAIL;
                    }
                }

                if (!TerritoryProtectionManager.canPlaceBlock(serverPlayer, serverWorld, clickedPos)
                        || !TerritoryProtectionManager.canPlaceBlock(serverPlayer, serverWorld, targetPos)) {
                    TerritoryProtectionManager.sendProtectedMessage(serverPlayer);
                    TerritoryProtectionManager.syncInventory(serverPlayer);
                    return ActionResult.FAIL;
                }

                return ActionResult.PASS;
            }

            BlockEntity blockEntity = world.getBlockEntity(clickedPos);

            if (blockEntity instanceof Inventory) {
                if (!ExplosionProtectionManager.canUseContainer(serverPlayer, clickedPos)) {
                    return ActionResult.FAIL;
                }

                if (!TerritoryProtectionManager.canOpenContainer(serverPlayer, serverWorld, clickedPos)) {
                    TerritoryProtectionManager.sendProtectedMessage(serverPlayer);
                    return ActionResult.FAIL;
                }
            }

            return ActionResult.PASS;
        });
    }

    private static void registerUseItemProtection() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);

            if (!isBucketItem(stack)) {
                return ActionResult.PASS;
            }

            HitResult hitResult = player.raycast(
                    5.0D,
                    0.0F,
                    stack.isOf(Items.BUCKET)
            );

            if (!(hitResult instanceof BlockHitResult blockHitResult)) {
                return ActionResult.PASS;
            }

            if (blockHitResult.getType() == HitResult.Type.MISS) {
                return ActionResult.PASS;
            }

            BlockPos clickedPos = blockHitResult.getBlockPos();
            BlockPos targetPos = clickedPos.offset(blockHitResult.getSide());

            if (!canUseFireOrFluid(serverPlayer, serverWorld, clickedPos, targetPos)) {
                TerritoryProtectionManager.syncInventory(serverPlayer);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    private static boolean canUseFireOrFluid(
            ServerPlayerEntity player,
            ServerWorld world,
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
                || block instanceof AbstractSignBlock
                || block instanceof LecternBlock;
    }

    private static boolean canUseProtectedInteractionBlock(
            ServerPlayerEntity player,
            ServerWorld world,
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

        if (block instanceof AbstractSignBlock) {
            return TerritoryProtectionManager.canPlaceBlock(player, world, pos);
        }

        if (block instanceof LecternBlock) {
            return TerritoryProtectionManager.canOpenContainer(player, world, pos);
        }

        return true;
    }

    private static void sendProtectedMessageOnlyOutsideSpawn(
            ServerPlayerEntity player,
            ServerWorld world,
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
                && stack.isOf(Items.GLOWSTONE);
    }

    private static boolean isFireOrFluidItem(ItemStack stack) {
        return stack.isOf(Items.FLINT_AND_STEEL)
                || stack.isOf(Items.FIRE_CHARGE)
                || isBucketItem(stack);
    }

    private static boolean isBucketItem(ItemStack stack) {
        return stack.isOf(Items.LAVA_BUCKET)
                || stack.isOf(Items.WATER_BUCKET)
                || stack.isOf(Items.BUCKET);
    }
}
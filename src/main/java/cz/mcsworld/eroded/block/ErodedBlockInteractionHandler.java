package cz.mcsworld.eroded.block;

import cz.mcsworld.eroded.protection.TerritoryClaim;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public final class ErodedBlockInteractionHandler {

    private ErodedBlockInteractionHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player == null || player.isSpectator()) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (!state.contains(ErodedBlock.VARIANT)) {
                return ActionResult.PASS;
            }

            if (!player.isSneaking()
                    && !player.isInSneakingPose()
                    && !player.shouldCancelInteraction()) {
                return ActionResult.PASS;
            }

            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }

            if (!(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            if (!canRotateErodedBlock(serverPlayer, serverWorld, pos)) {
                sendRotateDeniedMessage(serverPlayer, serverWorld, pos);
                TerritoryProtectionManager.syncInventory(serverPlayer);
                return ActionResult.FAIL;
            }

            int currentVariant = state.get(ErodedBlock.VARIANT);
            int nextVariant = currentVariant >= 23 ? 0 : currentVariant + 1;

            serverWorld.setBlockState(
                    pos,
                    state.with(ErodedBlock.VARIANT, nextVariant),
                    Block.NOTIFY_ALL_AND_REDRAW
            );

            return ActionResult.SUCCESS;
        });
    }

    private static boolean canRotateErodedBlock(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {

        if (ExplosionProtectionManager.isProtected(world, pos)) {
            return ExplosionProtectionManager.canBreak(player, pos)
                    && ExplosionProtectionManager.canPlace(player, pos);
        }

        return TerritoryProtectionManager.canBreakBlock(player, world, pos)
                && TerritoryProtectionManager.canPlaceBlock(player, world, pos);
    }

    private static void sendRotateDeniedMessage(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        if (ExplosionProtectionManager.isProtected(world, pos)) {
            return;
        }

        TerritoryClaim anchorClaim = TerritoryProtectionManager.getAnchorClaim(world, pos);

        if (anchorClaim != null) {
            TerritoryProtectionManager.sendAnchorOwnerOnlyMessage(player);
            return;
        }

        TerritoryProtectionManager.sendProtectedMessage(player);
    }
}
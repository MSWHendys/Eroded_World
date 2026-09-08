package cz.mcsworld.eroded.block;

import cz.mcsworld.eroded.protection.TerritoryClaim;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ErodedBlockInteractionHandler {

    private ErodedBlockInteractionHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.isSpectator()) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (!state.hasProperty(ErodedBlock.VARIANT)) {
                return InteractionResult.PASS;
            }

            if (!player.isShiftKeyDown()
                    && !player.isCrouching()
                    && !player.isSecondaryUseActive()) {
                return InteractionResult.PASS;
            }

            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            if (!(world instanceof ServerLevel serverWorld)
                    || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            if (!canRotateErodedBlock(serverPlayer, serverWorld, pos)) {
                sendRotateDeniedMessage(serverPlayer, serverWorld, pos);
                TerritoryProtectionManager.syncInventory(serverPlayer);
                return InteractionResult.FAIL;
            }

            int currentVariant = state.getValue(ErodedBlock.VARIANT);
            int nextVariant = currentVariant >= 23 ? 0 : currentVariant + 1;

            serverWorld.setBlock(
                    pos,
                    state.setValue(ErodedBlock.VARIANT, nextVariant),
                    Block.UPDATE_ALL_IMMEDIATE
            );

            return InteractionResult.SUCCESS;
        });
    }

    private static boolean canRotateErodedBlock(
            ServerPlayer player,
            ServerLevel world,
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
            ServerPlayer player,
            ServerLevel world,
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
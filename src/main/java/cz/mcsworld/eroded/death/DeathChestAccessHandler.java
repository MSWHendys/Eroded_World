package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public final class DeathChestAccessHandler {

    private DeathChestAccessHandler() {}

    public static void register() {
        UseBlockCallback.EVENT.register(DeathChestAccessHandler::onUse);
    }

    private static InteractionResult onUse(
            Player player,
            Level world,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hit.getBlockPos();

        if (!world.getBlockState(pos).is(ErodedBlocks.DEATH_ENDER_CHEST)) {
            return InteractionResult.PASS;
        }

        DeathChestState state = DeathChestState.get((ServerLevel) world);
        DeathChestState.Entry entry = state.get(pos);

        if (entry == null) return InteractionResult.PASS;

        if (!state.isProtected(pos)) {
            return InteractionResult.PASS;
        }

        if (serverPlayer.getUUID().equals(entry.owner())) {
            return InteractionResult.PASS;
        }

        serverPlayer.sendOverlayMessage(
                Component.translatable("eroded.death.chest.protected")
        );

        return InteractionResult.FAIL;
    }
}

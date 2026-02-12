package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DeathChestAccessHandler {

    private DeathChestAccessHandler() {}

    public static void register() {
        UseBlockCallback.EVENT.register(DeathChestAccessHandler::onUse);
    }

    private static ActionResult onUse(
            PlayerEntity player,
            World world,
            Hand hand,
            BlockHitResult hit
    ) {
        if (world.isClient()) return ActionResult.PASS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        BlockPos pos = hit.getBlockPos();

        if (!world.getBlockState(pos).isOf(ErodedBlocks.DEATH_ENDER_CHEST)) {
            return ActionResult.PASS;
        }

        DeathChestState state = DeathChestState.get((ServerWorld) world);
        DeathChestState.Entry entry = state.get(pos);

        if (entry == null) return ActionResult.PASS;

        if (!state.isProtected(pos)) {
            return ActionResult.PASS;
        }

        if (serverPlayer.getUuid().equals(entry.owner())) {
            return ActionResult.PASS;
        }

        serverPlayer.sendMessage(
                Text.translatable("eroded.death.chest.protected"),
                true
        );

        return ActionResult.FAIL;
    }
}

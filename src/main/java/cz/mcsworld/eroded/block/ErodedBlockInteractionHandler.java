package cz.mcsworld.eroded.block;


import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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

            if (!player.isSneaking() && !player.isInSneakingPose() && !player.shouldCancelInteraction()) {
                return ActionResult.PASS;
            }

            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }

            int currentVariant = state.get(ErodedBlock.VARIANT);
            int nextVariant = currentVariant >= 23 ? 0 : currentVariant + 1;

            world.setBlockState(
                    pos,
                    state.with(ErodedBlock.VARIANT, nextVariant),
                    Block.NOTIFY_ALL_AND_REDRAW
            );


            return ActionResult.SUCCESS;
        });
    }
}
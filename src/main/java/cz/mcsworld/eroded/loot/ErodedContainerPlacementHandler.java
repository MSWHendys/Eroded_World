package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;

public class ErodedContainerPlacementHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!(world instanceof ServerLevel serverWorld)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) return InteractionResult.PASS;

            if (!(stack.getItem() instanceof BlockItem blockItem)) return InteractionResult.PASS;
            Block block = blockItem.getBlock();
            if (!(block instanceof ChestBlock) && !(block instanceof BarrelBlock)) return InteractionResult.PASS;

            BlockPos placePos = hit.getBlockPos().relative(hit.getDirection());
            long posKey = placePos.asLong();

            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            boolean hasAdminTag = data != null && data.copyTag().getBoolean("eroded_loot_chest").orElse(false);

            if (hasAdminTag) {

                ErodedLootState.get(serverWorld).unmarkPlayerPlaced(posKey);
                ErodedLootState.get(serverWorld).markAdminPlaced(posKey);
            } else {

                ErodedLootState.get(serverWorld).markPlayerPlaced(posKey);
                ErodedLootState.get(serverWorld).unmarkAdminPlaced(posKey);
            }

            return InteractionResult.PASS;
        });
    }
}
package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class ErodedContainerPlacementHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(world instanceof ServerWorld serverWorld)) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (stack.isEmpty()) return ActionResult.PASS;

            if (!(stack.getItem() instanceof BlockItem blockItem)) return ActionResult.PASS;
            Block block = blockItem.getBlock();
            if (!(block instanceof ChestBlock) && !(block instanceof BarrelBlock)) return ActionResult.PASS;

            BlockPos placePos = hit.getBlockPos().offset(hit.getSide());
            long posKey = placePos.asLong();

            NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
            boolean hasAdminTag = data != null && data.copyNbt().getBoolean("eroded_loot_chest").orElse(false);

            if (hasAdminTag) {

                ErodedLootState.get(serverWorld).unmarkPlayerPlaced(posKey);
                ErodedLootState.get(serverWorld).markAdminPlaced(posKey);
            } else {

                ErodedLootState.get(serverWorld).markPlayerPlaced(posKey);
                ErodedLootState.get(serverWorld).unmarkAdminPlaced(posKey);
            }

            return ActionResult.PASS;
        });
    }
}
package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class ErodedContainerHandler {

    public static void register() {

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {

            if (world.isClient()) return ActionResult.PASS;

            BlockPos pos = hit.getBlockPos();
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof ChestBlockEntity chest) {

                Inventory inv = ChestBlock.getInventory(
                        (ChestBlock) chest.getCachedState().getBlock(),
                        chest.getCachedState(),
                        world,
                        pos,
                        true
                );

                if (inv == null) return ActionResult.PASS;

                ErodedLootManager.handleOpen(player, (ServerWorld) world, pos, inv);
            }

            if (be instanceof BarrelBlockEntity barrel) {

                ErodedLootManager.handleOpen(player, (ServerWorld) world, pos, barrel);
            }

            return ActionResult.PASS;
        });
    }
}
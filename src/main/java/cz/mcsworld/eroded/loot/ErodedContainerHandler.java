package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class ErodedContainerHandler {

    public static void register() {

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {

            if (world.isClientSide()) return InteractionResult.PASS;

            BlockPos pos = hit.getBlockPos();
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof ChestBlockEntity chest) {

                Container inv = ChestBlock.getContainer(
                        (ChestBlock) chest.getBlockState().getBlock(),
                        chest.getBlockState(),
                        world,
                        pos,
                        true
                );

                if (inv == null) return InteractionResult.PASS;

                ErodedLootManager.handleOpen(player, (ServerLevel) world, pos, inv);
            }

            if (be instanceof BarrelBlockEntity barrel) {
                ErodedLootManager.handleOpen(player, (ServerLevel) world, pos, barrel);
            }

            return InteractionResult.PASS;
        });
    }
}

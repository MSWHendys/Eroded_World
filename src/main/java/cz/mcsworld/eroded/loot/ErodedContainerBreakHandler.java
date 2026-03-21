package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.server.world.ServerWorld;

public class ErodedContainerBreakHandler {

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;

            if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
                ErodedLootState lootState = ErodedLootState.get(serverWorld);
                long posKey = pos.asLong();

                lootState.unmarkPlayerPlaced(posKey);
                lootState.unmarkAdminPlaced(posKey);
                lootState.clearOpenedHistory(posKey);
            }
        });
    }
}
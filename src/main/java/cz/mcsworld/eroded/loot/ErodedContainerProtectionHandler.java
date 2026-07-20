package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;

public class ErodedContainerProtectionHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel serverWorld)) return true;

            if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
                if (player.hasPermissions(2)) return true;

                ErodedLootState lootState = ErodedLootState.get(serverWorld);
                long posKey = pos.asLong();

                if (lootState.isAdminPlaced(posKey)) {
                    player.displayClientMessage(Component.translatable("eroded.loot.chest.protected"), true);
                    return false;
                }

                if (lootState.isErodedGenerated(posKey)) {
                    player.displayClientMessage(Component.translatable("eroded.loot.chest.protected"), true);
                    return false;
                }

                if (!lootState.isPlayerPlaced(posKey) && !lootState.hasAnyPlayerOpened(posKey)) {
                    player.displayClientMessage(Component.translatable("eroded.loot.chest.protected"), true);
                    return false;
                }

            }
            return true;
        });
    }
}
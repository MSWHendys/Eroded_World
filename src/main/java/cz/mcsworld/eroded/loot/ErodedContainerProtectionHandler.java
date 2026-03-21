package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class ErodedContainerProtectionHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) return true;

            if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
                if (player.hasPermissionLevel(2)) return true;

                ErodedLootState lootState = ErodedLootState.get(serverWorld);
                long posKey = pos.asLong();

                if (lootState.isAdminPlaced(posKey)) {
                    player.sendMessage(Text.translatable("eroded.loot.chest.protected"), true);
                    return false;
                }

                if (lootState.isErodedGenerated(posKey)) {
                    player.sendMessage(Text.translatable("eroded.loot.chest.protected"), true);
                    return false;
                }

                if (!lootState.isPlayerPlaced(posKey) && !lootState.hasAnyPlayerOpened(posKey)) {
                    player.sendMessage(Text.translatable("eroded.loot.chest.protected"), true);
                    return false;
                }

            }
            return true;
        });
    }
}
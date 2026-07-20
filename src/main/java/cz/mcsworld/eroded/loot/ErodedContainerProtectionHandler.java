package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;

public class ErodedContainerProtectionHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel serverWorld)) return true;

            if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
                if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    return true;
                }

                ErodedLootState lootState = ErodedLootState.get(serverWorld);
                long posKey = pos.asLong();

                if (lootState.isAdminPlaced(posKey)) {
                    player.sendSystemMessage(Component.translatable("eroded.loot.chest.protected"));
                    return false;
                }

                if (lootState.isErodedGenerated(posKey)) {
                    player.sendSystemMessage(Component.translatable("eroded.loot.chest.protected"));
                    return false;
                }

                if (!lootState.isPlayerPlaced(posKey) && !lootState.hasAnyPlayerOpened(posKey)) {
                    player.sendSystemMessage(Component.translatable("eroded.loot.chest.protected"));
                    return false;
                }

            }
            return true;
        });
    }
}
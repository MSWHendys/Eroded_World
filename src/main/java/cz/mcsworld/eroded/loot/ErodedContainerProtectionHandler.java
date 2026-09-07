package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;

public class ErodedContainerProtectionHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, blockState, blockEntity) -> {
            if (!(world instanceof ServerLevel serverWorld)) return true;

            if (blockState.getBlock() instanceof ChestBlock || blockState.getBlock() instanceof BarrelBlock) {
                if (player.hasPermissions(2)) return true;

                ErodedLootState lootState = ErodedLootState.get(serverWorld);
                long key = lootState.normalize(ErodedContainerIdentity.resolve(serverWorld, pos, blockState));

                if (lootState.isAdminPlaced(key)
                        || lootState.isErodedGenerated(key)
                        || (!lootState.isPlayerPlaced(key) && !lootState.hasAnyPlayerOpened(key))) {
                    player.displayClientMessage(Component.translatable("eroded.loot.chest.protected"), true);
                    return false;
                }
            }
            return true;
        });
    }
}

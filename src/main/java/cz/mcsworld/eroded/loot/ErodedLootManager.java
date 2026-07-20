package cz.mcsworld.eroded.loot;

import cz.mcsworld.eroded.config.loot.LootConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public class ErodedLootManager {

    public static void handleOpen(Player player, ServerLevel world, BlockPos pos, Container inv) {
        LootConfig config = LootConfig.get();
        if (!config.enabled) return;

        ErodedLootState state = ErodedLootState.get(world);
        long key = pos.asLong();

        if (state.isPlayerPlaced(key)) return;
        if (state.hasOpened(key, player.getUUID())) return;

        if (state.isAdminPlaced(key)) {
            ErodedLootGenerator.generate(inv);
        } else {

            if (state.isErodedGenerated(key)) {
                ErodedLootGenerator.generate(inv);
            }

            else if (!state.hasAnyPlayerOpened(key)) {
                if (world.random.nextDouble() <= config.erodedLootChance) {
                    state.markErodedGenerated(key);
                    ErodedLootGenerator.generate(inv);
                }
            }
        }

        state.markOpened(key, player.getUUID());
    }
}
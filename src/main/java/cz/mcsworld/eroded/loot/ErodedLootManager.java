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
        long key = state.normalize(ErodedContainerIdentity.resolve(world, pos));

        if (state.isPlayerPlaced(key)) return;
        if (state.hasOpened(key, player.getUUID())) return;

        boolean adminPlaced = state.isAdminPlaced(key);
        boolean generated = state.isErodedGenerated(key);

        // A normal world chest is rolled only once globally. After that first
        // decision there is no reason to retain every future player's UUID.
        if (!adminPlaced && !generated && state.hasAnyPlayerOpened(key)) {
            state.compactOpenedHistory(key);
            return;
        }

        if (adminPlaced) {
            ErodedLootGenerator.generate(inv);
        } else if (generated) {
            ErodedLootGenerator.generate(inv);
        } else if (!state.hasAnyPlayerOpened(key)) {
            if (world.getRandom().nextDouble() <= config.erodedLootChance) {
                state.markErodedGenerated(key);
                ErodedLootGenerator.generate(inv);
            }
        }

        state.markOpened(key, player.getUUID());
    }
}

package cz.mcsworld.eroded.loot;

import cz.mcsworld.eroded.config.loot.LootConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ErodedLootManager {

    public static void handleOpen(PlayerEntity player, ServerWorld world, BlockPos pos, Inventory inv) {
        LootConfig config = LootConfig.get();
        if (!config.enabled) return;

        ErodedLootState state = ErodedLootState.get(world);
        long key = pos.asLong();

        if (state.isPlayerPlaced(key)) return;
        if (state.hasOpened(key, player.getUuid())) return;

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

        state.markOpened(key, player.getUuid());
    }
}
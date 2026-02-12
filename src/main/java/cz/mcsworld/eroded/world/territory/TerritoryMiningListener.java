package cz.mcsworld.eroded.world.territory;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

public final class TerritoryMiningListener {

    private TerritoryMiningListener() {}

    public static void register() {

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;
            ChunkPos chunk = new ChunkPos(pos);
            TerritoryCellKey key =
                    TerritoryCellKey.fromChunk(chunk.x, chunk.z);
            TerritoryWorldState stateData =
                    TerritoryWorldState.get(serverWorld);
            TerritoryCell cell =
                    stateData.getOrCreateCell(key);
            cell.incrementMining();
            stateData.markDirty();
        });
    }
}

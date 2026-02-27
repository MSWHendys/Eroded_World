package cz.mcsworld.eroded.world.territory;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

public final class TerritoryMiningListener {

    private TerritoryMiningListener() {}

    public static void register() {

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, blockState, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;

            long tick = serverWorld.getServer().getTicks();
            TerritoryTracker.onBlockBroken(serverWorld, pos, blockState);
            ChunkPos chunk = new ChunkPos(pos);
            TerritoryCellKey key = TerritoryCellKey.fromChunk(chunk.x, chunk.z);

            TerritoryWorldState stateData = TerritoryWorldState.get(serverWorld);
            TerritoryCell cell = stateData.getOrCreateCell(key);

            cell.incrementMiningScore();
            cell.addMining(1, tick);
            stateData.markDirty();
            cell.setLastMiningActivityTick(tick);
        });
    }
}
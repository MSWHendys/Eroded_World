package cz.mcsworld.eroded.world.territory;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class TerritoryMiningListener {

    private TerritoryMiningListener() {}

    public static void register() {

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, blockState, blockEntity) -> {
            if (!(world instanceof ServerLevel serverWorld)) return;

            long tick = serverWorld.getGameTime();
            TerritoryTracker.onBlockBroken(serverWorld, pos, blockState);
            ChunkPos chunk = new ChunkPos(
                    player.blockPosition().getX() >> 4,
                    player.blockPosition().getZ() >> 4);
            TerritoryCellKey key = TerritoryCellKey.fromChunk(chunk.x(), chunk.z());

            TerritoryWorldState stateData = TerritoryWorldState.get(serverWorld);
            TerritoryCell cell = stateData.getOrCreateCell(key);

            cell.incrementMiningScore();
            cell.addMining(1, tick);
            stateData.setDirty();
            cell.setLastMiningActivityTick(tick);
            cell.touchActivity(tick);
        });
    }
}
package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.world.territory.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;


public final class MutatedMobResolver {

    public static final String MUTATED_TAG = "eroded_mutated";

    private MutatedMobResolver() {}

    public static boolean shouldBeMutated(ServerLevel world, Monster mob) {

        int chunkX = mob.blockPosition().getX() >> 4;
        int chunkZ = mob.blockPosition().getZ() >> 4;

        ChunkPos cp = new ChunkPos(chunkX, chunkZ);

        TerritoryWorldState worldState =
                TerritoryWorldState.get(world);

        TerritoryCellKey key =
                TerritoryCellKey.fromChunk(cp.x(), cp.z());

        TerritoryCell cell =
                worldState.getOrCreateCell(key);

        long tick = world.getServer().getTickCount();

        float threat =
                TerritoryThreatResolver.computeThreat(cell, tick);

        if (threat < 0.65f) return false;

        float chance = (threat - 0.65f) * 2.0f;
        return world.getRandom().nextFloat() < chance;

    }

}

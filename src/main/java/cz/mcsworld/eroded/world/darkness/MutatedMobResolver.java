package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.world.territory.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;


public final class MutatedMobResolver {

    public static final String MUTATED_TAG = "eroded_mutated";

    private MutatedMobResolver() {}

    public static boolean shouldBeMutated(ServerWorld world, HostileEntity mob) {

        ChunkPos cp = new ChunkPos(mob.getBlockPos());

        TerritoryWorldState worldState =
                TerritoryWorldState.get(world);

        TerritoryCellKey key =
                TerritoryCellKey.fromChunk(cp.x, cp.z);

        TerritoryCell cell =
                worldState.getOrCreateCell(key);

        long tick = world.getServer().getTicks();

        float threat =
                TerritoryThreatResolver.computeThreat(cell, tick);

        if (threat < 0.65f) return false;

        float chance = (threat - 0.65f) * 2.0f;
        System.out.println("[Eroded] Mutation check | Threat=" + threat + " | Chance=" + chance);
        return world.random.nextFloat() < chance;

    }

}

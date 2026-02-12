package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.world.territory.TerritoryData;
import cz.mcsworld.eroded.world.territory.TerritoryStorage;
import cz.mcsworld.eroded.world.territory.TerritoryThreatResolver;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

public final class MutatedMobResolver {

    public static final String MUTATED_TAG = "eroded_mutated";

    private MutatedMobResolver() {}

    public static boolean shouldBeMutated(ServerWorld world, HostileEntity mob) {

        ChunkPos cp = new ChunkPos(mob.getBlockPos());
        TerritoryData data = TerritoryStorage.get(world, cp);

        long tick = world.getServer().getTicks();
        float threat = TerritoryThreatResolver.computeThreat(data, tick);

        if (threat < 0.65f) return false;

        float chance = (threat - 0.65f) * 2.0f;

        return world.random.nextFloat() < chance;
    }

}

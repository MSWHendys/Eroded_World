package cz.mcsworld.eroded.death;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DeathDistanceHelper {

    private DeathDistanceHelper() {}

    public static double getDeathToWorldSpawnDistance(
            ServerPlayerEntity player,
            BlockPos deathPos,
            RegistryKey<World> deathDim
    ) {
        ServerWorld world = player.getWorld();
        BlockPos spawnPos = world.getSpawnPos();
        RegistryKey<World> spawnDim = world.getRegistryKey();

        if (!deathDim.equals(spawnDim)) {
            return -1;
        }

        double dx = deathPos.getX() - spawnPos.getX();
        double dz = deathPos.getZ() - spawnPos.getZ();

        return Math.sqrt(dx * dx + dz * dz);
    }
}

package cz.mcsworld.eroded.death;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class DeathDistanceHelper {

    private DeathDistanceHelper() {}

    public static double getDeathToWorldSpawnDistance(
            ServerPlayer player,
            BlockPos deathPos,
            ResourceKey<@NotNull Level> deathDim
    ) {
        ServerLevel world = player.level();
        BlockPos spawnPos = world.getRespawnData().pos();
        ResourceKey<@NotNull Level> spawnDim = world.dimension();

        if (!deathDim.equals(spawnDim)) {
            return -1;
        }

        double dx = deathPos.getX() - spawnPos.getX();
        double dz = deathPos.getZ() - spawnPos.getZ();

        return Math.sqrt(dx * dx + dz * dz);
    }
}

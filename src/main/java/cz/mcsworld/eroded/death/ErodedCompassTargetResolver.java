package cz.mcsworld.eroded.death;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ErodedCompassTargetResolver {

    private ErodedCompassTargetResolver() {}

    public static BlockPos resolveTarget(
            ServerPlayerEntity player,
            ErodedDeathMemory memory
    ) {
        ServerWorld playerWorld = player.getWorld();
        RegistryKey<World> playerDim = playerWorld.getRegistryKey();
        RegistryKey<World> deathDim  = memory.getDeathDimension();

        if (playerDim.equals(deathDim)) {
            return memory.getDeathPos();
        }

        MinecraftServer server = player.getServer();
        if (server == null) return memory.getDeathPos();

        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) return memory.getDeathPos();

        if (deathDim.equals(World.NETHER) && !playerDim.equals(World.NETHER)) {
            BlockPos portal = ErodedPortalMemoryState.get(overworld).getOverworldPortal(player.getUuid());
            return portal != null ? portal : overworld.getSpawnPos();
        }

        if (deathDim.equals(World.END) && !playerDim.equals(World.END)) {
            return overworld.getSpawnPos();
        }

        return memory.getDeathPos();
    }
}

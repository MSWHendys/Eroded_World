package cz.mcsworld.eroded.death;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class ErodedCompassTargetResolver {

    private ErodedCompassTargetResolver() {}

    public static BlockPos resolveTarget(
            ServerPlayer player,
            ErodedDeathMemory memory
    ) {
        ServerLevel playerWorld = player.level();
        ResourceKey<Level> playerDim = playerWorld.dimension();
        ResourceKey<Level> deathDim  = memory.getDeathDimension();

        if (playerDim.equals(deathDim)) {
            return memory.getDeathPos();
        }

        MinecraftServer server = player.getServer();
        if (server == null) return memory.getDeathPos();

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return memory.getDeathPos();

        if (deathDim.equals(Level.NETHER) && !playerDim.equals(Level.NETHER)) {
            BlockPos portal = ErodedPortalMemoryState.get(overworld).getOverworldPortal(player.getUUID());
            return portal != null ? portal : overworld.getSharedSpawnPos();
        }

        if (deathDim.equals(Level.END) && !playerDim.equals(Level.END)) {
            return overworld.getSharedSpawnPos();
        }

        return memory.getDeathPos();
    }
}

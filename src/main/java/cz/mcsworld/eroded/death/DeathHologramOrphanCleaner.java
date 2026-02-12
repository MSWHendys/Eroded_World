package cz.mcsworld.eroded.death;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;


import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DeathHologramOrphanCleaner {

    private static final String TAG_HOLOGRAM_ID = "hid_";

    private DeathHologramOrphanCleaner() {}

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register(
                DeathHologramOrphanCleaner::onChunkLoad
        );
    }

    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {

        Set<UUID> validIds = DeathChestState.get(world)
                .all()
                .stream()
                .map(DeathChestState.Entry::hologramId)
                .collect(Collectors.toSet());

        ChunkPos cPos = chunk.getPos();

        int bottomY = world.getBottomY();
        int topY = world.getTopYInclusive();

        Box chunkBox = new Box(
                cPos.getStartX(), bottomY, cPos.getStartZ(),
                cPos.getEndX() + 1, topY + 1, cPos.getEndZ() + 1
        );

        for (Entity e : world.getOtherEntities(null, chunkBox)) {

            if (e.getCommandTags().isEmpty()) continue;

            for (String tag : e.getCommandTags()) {
                if (tag.startsWith(TAG_HOLOGRAM_ID)) {
                    try {
                        UUID id = UUID.fromString(tag.substring(TAG_HOLOGRAM_ID.length()));

                        if (!validIds.contains(id)) {
                            e.discard();
                        }
                    } catch (IllegalArgumentException ignored) {

                        e.discard();
                    }
                }
            }
        }
    }
}
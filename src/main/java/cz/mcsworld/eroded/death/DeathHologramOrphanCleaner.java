package cz.mcsworld.eroded.death;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DeathHologramOrphanCleaner {

    private static final String TAG_HOLOGRAM_ID = "hid_";

    private DeathHologramOrphanCleaner() {}

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk, ignored) ->
                onChunkLoad(world, chunk)
        );
    }

    private static void onChunkLoad(ServerLevel world, LevelChunk chunk) {

        Set<UUID> validIds = DeathChestState.get(world)
                .all()
                .stream()
                .map(DeathChestState.Entry::hologramId)
                .collect(Collectors.toSet());

        ChunkPos cPos = chunk.getPos();

        int bottomY = world.getMinY();
        int topY = world.getMaxY();

        AABB chunkBox = new AABB(
                cPos.getMinBlockX(), bottomY, cPos.getMinBlockZ(),
                cPos.getMaxBlockX() + 1, topY + 1, cPos.getMaxBlockZ() + 1
        );

        for (Entity e : world.getEntities(null, chunkBox)) {

            if (e.entityTags().isEmpty()) continue;

            for (String tag : e.entityTags()) {
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
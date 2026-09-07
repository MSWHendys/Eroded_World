package cz.mcsworld.eroded.world.darkness;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared ownership/ref-count manager for Warding Lamp and held Eroded Torch
 * LIGHT blocks. It never claims an external minecraft:light block unless that
 * coordinate is explicitly recorded as Eroded-managed in SavedData.
 */
public final class DynamicLightManager {

    public enum Source {
        WARDING_LAMP,
        ERODED_TORCH
    }

    private record Owner(UUID player, Source source) {
    }

    private record LightKey(ResourceKey<Level> worldKey, BlockPos pos) {
    }

    private static final class Lease {
        private final Map<Owner, Integer> requestedLevels = new HashMap<>();
        private int appliedLevel;

        private Lease(int appliedLevel) {
            this.appliedLevel = appliedLevel;
        }

        private int desiredLevel() {
            int max = 1;
            for (int level : requestedLevels.values()) max = Math.max(max, level);
            return Mth.clamp(max, 1, 15);
        }
    }

    private static final Map<Owner, LightKey> OWNER_LIGHTS = new HashMap<>();
    private static final Map<LightKey, Lease> LEASES = new HashMap<>();

    private DynamicLightManager() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register(DynamicLightManager::cleanupStaleInChunk);
        ServerLifecycleEvents.SERVER_STOPPING.register(DynamicLightManager::releaseAll);
    }

    public static boolean canUse(ServerLevel world, BlockPos pos, UUID player, Source source) {
        if (world.isEmptyBlock(pos)) return true;
        if (!world.getBlockState(pos).is(Blocks.LIGHT)) return false;

        LightKey key = new LightKey(world.dimension(), pos.immutable());
        if (LEASES.containsKey(key)) return true;

        DynamicLightState state = DynamicLightState.getIfPresent(world);
        if (state == null || !state.isManaged(pos.asLong())) return false;

        int expected = state.expectedLevel(pos.asLong());
        int currentLevel = world.getBlockState(pos).getValue(LightBlock.LEVEL);
        if (expected == currentLevel) return true;

        // The coordinate used to be ours, but somebody changed the LIGHT
        // level while no runtime lease exists. Treat it as external now.
        state.unmark(pos.asLong());
        return false;
    }

    public static boolean ensure(
            ServerLevel world,
            BlockPos pos,
            UUID player,
            Source source,
            int requestedLevel
    ) {
        Owner owner = new Owner(player, source);
        LightKey target = new LightKey(world.dimension(), pos.immutable());
        int safeLevel = Mth.clamp(requestedLevel, 1, 15);

        if (!canUse(world, pos, player, source)) return false;

        LightKey old = OWNER_LIGHTS.get(owner);
        if (old != null && !old.equals(target)) {
            release(owner, world.getServer());
        }

        Lease lease = LEASES.get(target);
        if (lease == null) {
            DynamicLightState persistent = DynamicLightState.getIfPresent(world);
            boolean persistedOwner = persistent != null && persistent.isManaged(pos.asLong());

            if (!world.isEmptyBlock(pos) && !persistedOwner) {
                // Existing unowned LIGHT belongs to a map/command/another mod.
                return false;
            }

            int initial = persistedOwner ? persistent.expectedLevel(pos.asLong()) : safeLevel;
            lease = new Lease(Mth.clamp(initial, 1, 15));
            LEASES.put(target, lease);
        }

        lease.requestedLevels.put(owner, safeLevel);
        OWNER_LIGHTS.put(owner, target);

        int desired = lease.desiredLevel();
        if (!applyManagedLight(world, pos, lease, desired)) {
            lease.requestedLevels.remove(owner);
            OWNER_LIGHTS.remove(owner);
            if (lease.requestedLevels.isEmpty()) LEASES.remove(target);
            return false;
        }

        return true;
    }

    public static void release(UUID player, Source source, MinecraftServer server) {
        release(new Owner(player, source), server);
    }

    private static void release(Owner owner, MinecraftServer server) {
        LightKey key = OWNER_LIGHTS.remove(owner);
        if (key == null) return;

        Lease lease = LEASES.get(key);
        if (lease == null) return;

        lease.requestedLevels.remove(owner);
        ServerLevel world = server.getLevel(key.worldKey());

        if (!lease.requestedLevels.isEmpty()) {
            if (world != null && world.hasChunkAt(key.pos())) {
                applyManagedLight(world, key.pos(), lease, lease.desiredLevel());
            }
            return;
        }

        LEASES.remove(key);
        if (world == null) return;

        // Never synchronously load a chunk just to remove an invisible light.
        // SavedData keeps ownership until CHUNK_LOAD can clean it safely.
        if (!world.hasChunkAt(key.pos())) return;

        removeIfStillOurs(world, key.pos(), lease.appliedLevel);
    }

    private static boolean applyManagedLight(ServerLevel world, BlockPos pos, Lease lease, int desiredLevel) {
        var current = world.getBlockState(pos);
        DynamicLightState state = DynamicLightState.get(world);
        long packed = pos.asLong();

        if (!world.isEmptyBlock(pos) && !current.is(Blocks.LIGHT)) {
            state.unmark(packed);
            return false;
        }

        if (current.is(Blocks.LIGHT)) {
            boolean ours = state.isManaged(packed);
            if (!ours) return false;
        }

        if (!current.is(Blocks.LIGHT)
                || current.getValue(LightBlock.LEVEL) != desiredLevel) {
            world.setBlock(
                    pos,
                    Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, desiredLevel),
                    Block.UPDATE_ALL
            );
        }

        lease.appliedLevel = desiredLevel;
        state.mark(packed, desiredLevel);
        return true;
    }

    private static void removeIfStillOurs(ServerLevel world, BlockPos pos, int expectedLevel) {
        DynamicLightState state = DynamicLightState.getIfPresent(world);
        if (state == null || !state.isManaged(pos.asLong())) return;

        var current = world.getBlockState(pos);
        int persistedLevel = state.expectedLevel(pos.asLong());
        int expected = persistedLevel >= 1 ? persistedLevel : expectedLevel;

        // If somebody replaced/edited our block, relinquish ownership but do
        // not destroy their content.
        if (current.is(Blocks.LIGHT)
                && current.getValue(LightBlock.LEVEL) == expected) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        state.unmark(pos.asLong());
    }

    private static void cleanupStaleInChunk(ServerLevel world, LevelChunk chunk) {
        DynamicLightState state = DynamicLightState.getIfPresent(world);
        if (state == null) return;

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        for (Map.Entry<Long, Integer> entry : state.snapshot().entrySet()) {
            BlockPos pos = BlockPos.of(entry.getKey());
            if ((pos.getX() >> 4) != chunkX || (pos.getZ() >> 4) != chunkZ) continue;

            LightKey key = new LightKey(world.dimension(), pos);
            if (LEASES.containsKey(key)) continue;

            removeIfStillOurs(world, pos, entry.getValue());
        }
    }

    private static void releaseAll(MinecraftServer server) {
        for (Owner owner : OWNER_LIGHTS.keySet().toArray(Owner[]::new)) {
            release(owner, server);
        }
        OWNER_LIGHTS.clear();
        LEASES.clear();
    }
}

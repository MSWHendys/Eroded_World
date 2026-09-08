package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.world.territory.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DarknessLightEater {

    private static int tickCounter = 0;
    private static int actions = 0;
    private static int zoneCursor = 0;

    private DarknessLightEater() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DarknessLightEater::onTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DarknessFlickerState.clearAll();
            tickCounter = 0;
            actions = 0;
            zoneCursor = 0;
        });
    }

    private record PlayerZone(ServerLevel world, ServerPlayer player) {}

    private static void onTick(MinecraftServer server) {
        var root = DarknessConfigs.get();
        var cfg = root.server;

        if (!root.enabled || !cfg.lightEaterEnabled) {
            return;
        }

        tickCounter++;
        if (tickCounter % cfg.lightEaterCheckInterval != 0) return;

        actions = 0;

        List<PlayerZone> zones = new ArrayList<>();
        Map<ServerLevel, Map<ChunkPos, Float>> threatCaches = new HashMap<>();
        Map<ServerLevel, Set<UUID>> processedByWorld = new HashMap<>();
        Map<ServerLevel, List<ServerPlayer>> lanternsByWorld = new HashMap<>();

        for (ServerLevel world : server.getAllLevels()) {
            List<ServerPlayer> lanternHolders = new ArrayList<>();
            for (ServerPlayer player : world.players()) {
                zones.add(new PlayerZone(world, player));
                if (isHoldingLantern(player)) {
                    lanternHolders.add(player);
                }
            }
            lanternsByWorld.put(world, lanternHolders);
        }

        if (zones.isEmpty()) {
            zoneCursor = 0;
            return;
        }

        int start = Math.floorMod(zoneCursor, zones.size());
        zoneCursor = (start + 1) % zones.size();

        for (int offset = 0; offset < zones.size(); offset++) {
            PlayerZone zone = zones.get((start + offset) % zones.size());
            ServerLevel world = zone.world();
            ServerPlayer player = zone.player();
            long tick = world.getGameTime();

            Map<ChunkPos, Float> threatCache = threatCaches.computeIfAbsent(world, ignored -> new HashMap<>());
            Set<UUID> processedMobs = processedByWorld.computeIfAbsent(world, ignored -> new HashSet<>());
            List<ServerPlayer> lanternHolders = lanternsByWorld.getOrDefault(world, List.of());

            AABB playerZone = new AABB(player.blockPosition()).inflate(48);
            for (Monster mob : world.getEntitiesOfClass(
                    Monster.class,
                    playerZone,
                    e -> e.entityTags().contains(MutatedMobResolver.MUTATED_TAG)
            )) {
                if (!processedMobs.add(mob.getUUID())) continue;
                if (actions >= cfg.maxLightActionsPerTick) return;
                tryExtinguish(world, mob, threatCache, lanternHolders, tick, cfg);
            }
        }
    }

    private static void tryExtinguish(
            ServerLevel world,
            Monster mob,
            Map<ChunkPos, Float> threatCache,
            List<ServerPlayer> lanternHolders,
            long tick,
            DarknessConfigs.Server cfg
    ) {
        BlockPos center = mob.blockPosition();

        for (ServerPlayer player : lanternHolders) {
            if (player.blockPosition().closerThan(center, 6)) {
                return;
            }
        }
        int chunkX = SectionPos.blockToSectionCoord(center.getX());
        int chunkZ = SectionPos.blockToSectionCoord(center.getZ());
        ChunkPos cp = new ChunkPos(chunkX, chunkZ);
        float threat = threatCache.computeIfAbsent(cp, c -> {
            TerritoryWorldState worldState = TerritoryWorldState.getIfPresent(world);
            if (worldState == null) return 0.0f;
            TerritoryCellKey key = TerritoryCellKey.fromChunk(c.x(), c.z());
            TerritoryCell cell = worldState.getCell(key);
            return cell == null ? 0.0f : TerritoryThreatResolver.computeThreat(cell, tick);
        });

        if (threat < cfg.threatRequired) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -cfg.lightEaterRadius; dx <= cfg.lightEaterRadius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -cfg.lightEaterRadius; dz <= cfg.lightEaterRadius; dz++) {

                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = world.getBlockState(pos);

                    if (!isLightSource(world, pos, state)) continue;

                    actions++;

                    boolean destroy = DarknessFlickerState.advance(world, pos);
                    DarknessFlickerEffects.play(world, pos);

                    if (destroy) {
                        DarknessLightConsumeEffects.play(world, pos);
                        world.destroyBlock(pos, false);

                        for (Monster nearby : world.getEntitiesOfClass(
                                Monster.class,
                                new AABB(pos).inflate(6),
                                e -> true
                        )) {
                            DarknessMobLightMemory.markLightExtinguished(nearby);
                        }

                        DarknessFlickerState.clear(world, pos);
                    } else {
                        applyDimVariant(world, pos, state);
                    }

                    return;
                }
            }
        }
    }

    private static boolean isHoldingLantern(ServerPlayer player) {
        return player.getMainHandItem().is(ErodedBlocks.WARDING_LANTERN.asItem()) ||
                player.getOffhandItem().is(ErodedBlocks.WARDING_LANTERN.asItem());
    }

    private static void applyDimVariant(ServerLevel world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        // A vanilla torch has no dimmed block-state. Keep it in place during
        // the warning/flicker stages and remove it only on the final stage.
        // The old AIR conversion made the next scan impossible and left stale
        // cache entries behind forever.
        if (isTorchBlock(block)) {
            return;
        }

        if (isCampfireBlock(block) && state.getValue(CampfireBlock.LIT)) {
            world.setBlock(
                    pos,
                    state.setValue(CampfireBlock.LIT, false),
                    Block.UPDATE_ALL
            );
        }
    }

    private static boolean isLightSource(ServerLevel world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        if (isCampfireBlock(block)) {
            // An already-unlit campfire is not a new light source. It remains
            // eligible only while finishing a flicker cycle that began when it
            // was lit.
            return state.getValue(CampfireBlock.LIT)
                    || DarknessFlickerState.has(world, pos);
        }

        return isTorchBlock(block)
                || block == Blocks.LANTERN
                || block == Blocks.SOUL_LANTERN;
    }

    private static boolean isTorchBlock(Block block) {
        return block == Blocks.TORCH
                || block == Blocks.WALL_TORCH
                || block == Blocks.SOUL_TORCH
                || block == Blocks.SOUL_WALL_TORCH;
    }

    private static boolean isCampfireBlock(Block block) {
        return block == Blocks.CAMPFIRE
                || block == Blocks.SOUL_CAMPFIRE;
    }
}

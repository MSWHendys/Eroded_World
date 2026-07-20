package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.world.territory.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.HashMap;
import java.util.Map;


public final class DarknessLightEater {

    private static int tickCounter = 0;
    private static int actions = 0;

    private DarknessLightEater() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DarknessLightEater::onTick);
    }

    private static void onTick(MinecraftServer server) {
        var root = DarknessConfigs.get();
        var cfg = root.server;

        if (!root.enabled || !cfg.lightEaterEnabled) {
            return;
        }


        tickCounter++;
        if (tickCounter % cfg.lightEaterCheckInterval != 0) return;

        actions = 0;

        for (ServerLevel world : server.getAllLevels()) {
            long tick = world.getServer().getTickCount();
            Map<ChunkPos, Float> threatCache = new HashMap<>();

            for (ServerPlayer player : world.players()) {
                AABB playerZone = new AABB(player.blockPosition()).inflate(48);

                for (Monster mob : world.getEntitiesOfClass(
                        Monster.class,
                        playerZone,
                        e -> e.getTags().contains(MutatedMobResolver.MUTATED_TAG)
                )) {
                    if (actions >= cfg.maxLightActionsPerTick) return;
                    tryExtinguish(world, mob, threatCache, tick, cfg);
                }
            }
        }
    }

    private static void tryExtinguish(
            ServerLevel world,
            Monster mob,
            Map<ChunkPos, Float> threatCache,
            long tick, DarknessConfigs.Server cfg
    ) {
        BlockPos center = mob.blockPosition();

        for (ServerPlayer player : world.players()) {
            if (player.blockPosition().closerThan(center, 6) && isHoldingLantern(player)) {
                return;
            }
        }

        ChunkPos cp = new ChunkPos(center);
        float threat = threatCache.computeIfAbsent(cp, c -> {
            TerritoryWorldState worldState = TerritoryWorldState.get(world);
            TerritoryCellKey key = TerritoryCellKey.fromChunk(c.x, c.z);
            TerritoryCell cell = worldState.getOrCreateCell(key);
            return TerritoryThreatResolver.computeThreat(cell, tick);
        });

        if (threat < cfg.threatRequired) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -cfg.lightEaterRadius; dx <= cfg.lightEaterRadius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -cfg.lightEaterRadius; dz <= cfg.lightEaterRadius; dz++) {

                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();

                    if (!isLightSource(block)) continue;

                    actions++;

                    if (!DarknessLightMemory.has(pos)) {
                        DarknessLightMemory.store(pos, state);
                    }

                    boolean destroy = DarknessFlickerState.advance(pos);
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

                        DarknessLightMemory.clear(pos);
                        DarknessFlickerState.clear(pos);
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

        if (block == Blocks.TORCH || block == Blocks.WALL_TORCH) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        if (block == Blocks.CAMPFIRE) {
            world.setBlock(
                    pos,
                    state.setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false),
                    Block.UPDATE_ALL
            );
        }
    }

    private static boolean isLightSource(Block block) {
        return block == Blocks.TORCH
                || block == Blocks.WALL_TORCH
                || block == Blocks.LANTERN
                || block == Blocks.SOUL_LANTERN
                || block == Blocks.CAMPFIRE;
    }
}
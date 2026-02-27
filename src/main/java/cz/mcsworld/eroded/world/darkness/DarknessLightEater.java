package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.world.territory.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;

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
        if (!root.enabled) return;

        var cfg = root.server;

        if (!DarknessConfigs.get().enabled) return;
        if (!cfg.lightEaterEnabled) return;
        tickCounter++;
        if (tickCounter % cfg.lightEaterCheckInterval != 0) return;

        actions = 0;

        for (ServerWorld world : server.getWorlds()) {

            long tick = world.getServer().getTicks();
            Map<ChunkPos, Float> threatCache = new HashMap<>();

            for (HostileEntity mob : world.getEntitiesByClass(
                    HostileEntity.class,
                    Box.of(Vec3d.of(world.getSpawnPos()), 256, 256, 256),
                    e -> e.getCommandTags().contains(MutatedMobResolver.MUTATED_TAG)
            )) {
                if (actions >= cfg.maxLightActionsPerTick) return;
                tryExtinguish(world, mob, threatCache, tick, cfg);
            }
        }
    }

    private static void tryExtinguish(
            ServerWorld world,
            HostileEntity mob,
            Map<ChunkPos, Float> threatCache,
            long tick, DarknessConfigs.Server cfg
    ) {

        BlockPos center = mob.getBlockPos();
        ChunkPos cp = new ChunkPos(center);

        float threat = threatCache.computeIfAbsent(cp, c -> {

            TerritoryWorldState worldState = TerritoryWorldState.get(world);

            TerritoryCellKey key =
                    TerritoryCellKey.fromChunk(c.x, c.z);

            TerritoryCell cell =
                    worldState.getOrCreateCell(key);

            return TerritoryThreatResolver.computeThreat(cell, tick);
        });


        if (threat < cfg.threatRequired) return;

        BlockPos.Mutable pos = new BlockPos.Mutable();

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
                        world.breakBlock(pos, false);

                        for (HostileEntity nearby : world.getEntitiesByClass(
                                HostileEntity.class,
                                new Box(pos).expand(6),
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

    private static void applyDimVariant(ServerWorld world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.TORCH || block == Blocks.WALL_TORCH) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }

        if (block == Blocks.CAMPFIRE) {
            world.setBlockState(
                    pos,
                    state.with(net.minecraft.block.CampfireBlock.LIT, false),
                    Block.NOTIFY_ALL
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

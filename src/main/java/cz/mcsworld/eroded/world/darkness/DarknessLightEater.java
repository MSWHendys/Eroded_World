package cz.mcsworld.eroded.world.darkness;

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

    private static final int CHECK_INTERVAL = 40;
    private static final int RADIUS = 2;
    private static final int MAX_ACTIONS_PER_TICK = 6;

    private static int tickCounter = 0;
    private static int actions = 0;

    private DarknessLightEater() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DarknessLightEater::onTick);
    }

    private static void onTick(MinecraftServer server) {

        tickCounter++;
        if (tickCounter % CHECK_INTERVAL != 0) return;

        actions = 0;

        for (ServerWorld world : server.getWorlds()) {

            long tick = world.getServer().getTicks();
            Map<ChunkPos, Float> threatCache = new HashMap<>();

            for (HostileEntity mob : world.getEntitiesByClass(
                    HostileEntity.class,
                    Box.of(Vec3d.of(world.getSpawnPos()), 256, 256, 256),
                    e -> e.getCommandTags().contains(MutatedMobResolver.MUTATED_TAG)
            )) {
                if (actions >= MAX_ACTIONS_PER_TICK) return;
                tryExtinguish(world, mob, threatCache, tick);
            }
        }
    }

    private static void tryExtinguish(
            ServerWorld world,
            HostileEntity mob,
            Map<ChunkPos, Float> threatCache,
            long tick
    ) {

        BlockPos center = mob.getBlockPos();
        ChunkPos cp = new ChunkPos(center);

        float threat = threatCache.computeIfAbsent(cp, c -> {
            TerritoryData d = TerritoryStorage.get(world, c);
            return TerritoryThreatResolver.computeThreat(d, tick);
        });

        if (threat < 0.6f) return;

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {

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

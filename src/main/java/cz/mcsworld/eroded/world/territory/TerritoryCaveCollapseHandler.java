package cz.mcsworld.eroded.world.territory;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class TerritoryCaveCollapseHandler {


    private static final Random RANDOM = new Random();


    private static final Map<TerritoryCellKey, Long> LAST_COLLAPSE =
            new HashMap<>();

    private TerritoryCaveCollapseHandler() {}

    private static final List<EntityType<? extends HostileEntity>> COLLAPSE_MOBS =
            List.of(
                    EntityType.ZOMBIE,
                    EntityType.SKELETON,
                    EntityType.SPIDER,
                    EntityType.CREEPER
            );
    public static void register() {
        var root = TerritoryConfig.get();
        var cfg = root.server;

        if (!cfg.enabled || !cfg.caveCollapseEnabled) return;
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {

            if (!(world instanceof ServerWorld serverWorld)) return;
            if (pos.getY() > cfg.collapseMaxY) return;

            ChunkPos chunk = new ChunkPos(pos);
            TerritoryCellKey key =
                    TerritoryCellKey.fromChunk(chunk.x, chunk.z);

            long now = System.currentTimeMillis();
            if (now - LAST_COLLAPSE.getOrDefault(key, 0L) < cfg.collapseCooldownMs) {
                return;
            }

            TerritoryWorldState stateData =
                    TerritoryWorldState.get(serverWorld);
            TerritoryCell cell =
                    stateData.getOrCreateCell(key);

            int score = cell.getMiningScore();

            if (score < cfg.miningThreshold) return;

            double chance = collapseChance(score);
            if (RANDOM.nextDouble() > chance) return;

            BlockPos collapseOrigin = pos.offset(
                    player.getHorizontalFacing().getOpposite(),
                    2
            );
            if (hasStabilizerNearby(serverWorld, collapseOrigin)) {

                serverWorld.playSound(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SoundEvents.ENTITY_CREAKING_AMBIENT,
                        SoundCategory.BLOCKS,
                        0.2f,
                        0.2f
                );

                return;
            }

            triggerCollapse(serverWorld, player, pos);
            LAST_COLLAPSE.put(key, now);



            if (RANDOM.nextFloat() < cfg.collapseMobSpawnChance) {
                trySpawnCollapseMob(serverWorld, player, pos);
            }
        });
    }

    private static double collapseChance(int miningScore) {
        var root = TerritoryConfig.get();
        var cfg = root.server;
        if (miningScore < 500) return cfg.collapseChanceLow; //0.005
        if (miningScore < 1000) return cfg.collapseChanceMid; //0.02
        return cfg.collapseChanceHigh;
    }

    private static boolean hasStabilizerNearby(ServerWorld world, BlockPos origin) {
        var root = TerritoryConfig.get();
        var cfg = root.server;
        BlockPos.Mutable check = new BlockPos.Mutable();

        for (int dx = -cfg.stabilizerRadius; dx <= cfg.stabilizerRadius; dx++) {
            for (int dy = -cfg.stabilizerRadius; dy <= cfg.stabilizerRadius; dy++) {
                for (int dz = -cfg.stabilizerRadius; dz <= cfg.stabilizerRadius; dz++) {

                    check.set(
                            origin.getX() + dx,
                            origin.getY() + dy,
                            origin.getZ() + dz
                    );

                    if (isStabilizer(world.getBlockState(check))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isStabilizer(BlockState state) {
        return state.isIn(BlockTags.LOGS);
    }

    private static void triggerCollapse(ServerWorld world, PlayerEntity player, BlockPos origin) {

        Direction behind =
                player.getHorizontalFacing().getOpposite();

        int widthRadius = 2;
        int height = 4;
        int depth = 6;
        int startOffset = 5;

        world.playSound(
                null,
                origin.getX() + 0.5,
                origin.getY() + 0.5,
                origin.getZ() + 0.5,
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.BLOCKS,
                0.7f,
                0.5f,
                world.getRandom().nextLong()
        );

        BlockPos dustOrigin = origin.offset(behind, startOffset);
        world.spawnParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                dustOrigin.getX() + 0.5,
                dustOrigin.getY() + 1.0,
                dustOrigin.getZ() + 0.5,
                20,
                1.0, 1.0, 1.0,
                0.05
        );

        for (int d = startOffset; d < startOffset + depth; d++) {

            BlockPos center =
                    origin.offset(behind, d);

            for (int w = -widthRadius; w <= widthRadius; w++) {
                for (int h = 1; h <= height; h++) {

                    BlockPos target = center.up(h);

                    if (behind.getAxis() == Direction.Axis.X) {
                        target = target.add(0, 0, w);
                    } else {
                        target = target.add(w, 0, 0);
                    }

                    BlockState state = world.getBlockState(target);

                    if (isCollapsable(state)) {

                        world.setBlockState(
                                target,
                                Blocks.GRAVEL.getDefaultState(),
                                2 | 16
                        );

                        if (RANDOM.nextInt(3) == 0) {
                            world.spawnParticles(
                                    new BlockStateParticleEffect(
                                            ParticleTypes.BLOCK,
                                            Blocks.GRAVEL.getDefaultState()
                                    ),
                                    target.getX() + 0.5,
                                    target.getY() + 0.5,
                                    target.getZ() + 0.5,
                                    1,
                                    0.1, 0.1, 0.1,
                                    0.05
                            );
                        }
                    }
                }
            }
        }
    }

    private static boolean isCollapsable(BlockState state) {
        return state.isOf(Blocks.STONE)
                || state.isOf(Blocks.DEEPSLATE)
                || state.isOf(Blocks.TUFF)
                || state.isOf(Blocks.ANDESITE)
                || state.isOf(Blocks.DIORITE)
                || state.isOf(Blocks.GRANITE);
    }

    private static void trySpawnCollapseMob(
            ServerWorld world,
            PlayerEntity player,
            BlockPos origin
    ) {

        Direction behind = player.getHorizontalFacing().getOpposite();

        for (int i = 0; i < TerritoryConfig.get().server.collapseMobSpawnAttempts; i++) {

            BlockPos basePos =
                    origin.offset(behind, 4 + RANDOM.nextInt(4));

            BlockPos spawnPos =
                    basePos.up(RANDOM.nextInt(2));

            if (!world.isAir(spawnPos) || !world.isAir(spawnPos.up())) {
                continue;
            }

            if (spawnPos.isWithinDistance(player.getBlockPos(), 6)) {
                continue;
            }

            EntityType<? extends HostileEntity> type =
                    COLLAPSE_MOBS.get(RANDOM.nextInt(COLLAPSE_MOBS.size()));


            HostileEntity mob = type.spawn(
                    world,
                    null,
                    spawnPos,
                    SpawnReason.EVENT,
                    true,
                    false
            );

            if (mob == null) return;

            return;
        }
    }

}

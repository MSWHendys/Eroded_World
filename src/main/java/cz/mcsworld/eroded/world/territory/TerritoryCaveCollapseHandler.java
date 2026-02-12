package cz.mcsworld.eroded.world.territory;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class TerritoryCaveCollapseHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger("Eroded-Collapse");

    private static final Random RANDOM = new Random();

    private static final long CELL_COOLDOWN_MS = 30_000;
    private static final int MINING_THRESHOLD = 300;
    private static final int STABILIZER_RADIUS = 3;
    private static final float MOB_SPAWN_CHANCE = 0.25f;
    private static final int MOB_SPAWN_ATTEMPTS = 5;

    private static final Map<TerritoryCellKey, Long> LAST_COLLAPSE =
            new HashMap<>();

    private TerritoryCaveCollapseHandler() {}
    private static final EntityType<? extends HostileEntity>[] COLLAPSE_MOBS = new EntityType[] {
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER
    };
    public static void register() {

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {

            if (!(world instanceof ServerWorld serverWorld)) return;
            if (pos.getY() > 50) return;

            ChunkPos chunk = new ChunkPos(pos);
            TerritoryCellKey key =
                    TerritoryCellKey.fromChunk(chunk.x, chunk.z);

            long now = System.currentTimeMillis();
            if (now - LAST_COLLAPSE.getOrDefault(key, 0L) < CELL_COOLDOWN_MS) {
                return;
            }

            TerritoryWorldState stateData =
                    TerritoryWorldState.get(serverWorld);
            TerritoryCell cell =
                    stateData.getOrCreateCell(key);

            int score = cell.getMiningScore();
            if (score < MINING_THRESHOLD) return;

            double chance = collapseChance(score);
            if (RANDOM.nextDouble() > chance) return;

            if (hasStabilizerNearby(serverWorld, pos)) {

                serverWorld.playSound(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SoundEvents.BLOCK_WOOD_PLACE,
                        SoundCategory.BLOCKS,
                        0.5f,
                        1.2f
                );

                LOGGER.debug("Collapse prevented by stabilizer at {}", pos);
                return;
            }

            triggerCollapse(serverWorld, player, pos);
            LAST_COLLAPSE.put(key, now);

            LOGGER.info("Collapse triggered at {} (score={})", pos, score);

            if (RANDOM.nextFloat() < MOB_SPAWN_CHANCE) {
                trySpawnCollapseMob(serverWorld, player, pos);
            }
        });
    }

    private static double collapseChance(int miningScore) {
        if (miningScore < 500) return 0.005;
        if (miningScore < 1000) return 0.02;
        return 0.05;
    }

    private static boolean hasStabilizerNearby(ServerWorld world, BlockPos origin) {

        BlockPos.Mutable check = new BlockPos.Mutable();

        for (int dx = -STABILIZER_RADIUS; dx <= STABILIZER_RADIUS; dx++) {
            for (int dy = -STABILIZER_RADIUS; dy <= STABILIZER_RADIUS; dy++) {
                for (int dz = -STABILIZER_RADIUS; dz <= STABILIZER_RADIUS; dz++) {

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
        return state.isOf(Blocks.OAK_LOG)
                || state.isOf(Blocks.STRIPPED_OAK_LOG);
    }

    private static void triggerCollapse(ServerWorld world, PlayerEntity player, BlockPos origin) {

        Direction behind =
                player.getHorizontalFacing().getOpposite();

        int widthRadius = 1;
        int height = 3;
        int depth = 4;
        int startOffset = 2;

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

        for (int i = 0; i < MOB_SPAWN_ATTEMPTS; i++) {

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
                    COLLAPSE_MOBS[RANDOM.nextInt(COLLAPSE_MOBS.length)];


            HostileEntity mob = type.spawn(
                    world,
                    null,
                    spawnPos,
                    SpawnReason.EVENT,
                    true,
                    false
            );

            if (mob == null) return;

            LOGGER.info(
                    "Collapse attracted {} at {}",
                    mob.getType().getTranslationKey(),
                    spawnPos
            );
            return;
        }
    }

}

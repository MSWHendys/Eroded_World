package cz.mcsworld.eroded.world.territory;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.world.darkness.MutatedMobResolver;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public final class TerritoryCaveCollapseHandler {

    private TerritoryCaveCollapseHandler() {
    }

    private static final List<EntityType<? extends Monster>> COLLAPSE_MOBS =
            List.of(
                    EntityType.ZOMBIE,
                    EntityType.SKELETON,
                    EntityType.SPIDER,
                    EntityType.CREEPER
            );

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {

            var cfg = TerritoryConfig.get().server;
            if (!cfg.enabled || !cfg.caveCollapseEnabled) return;
            if (!(world instanceof ServerLevel serverWorld)) return;

            if (!player.isCreative()) {
                handleMiningEnergy(player, state);
            }

            if (pos.getY() > cfg.collapseMaxY) return;

            ChunkPos chunk = new ChunkPos(pos);
            TerritoryCellKey key = TerritoryCellKey.fromChunk(chunk.x, chunk.z);

            TerritoryWorldState stateData = TerritoryWorldState.get(serverWorld);
            TerritoryCell cell = stateData.getOrCreateCell(key);

            long now = serverWorld.getGameTime();
            long cooldownTicks = cfg.collapseCooldownMs / 50;

            if (now - cell.getLastMiningActivityTick() < cooldownTicks) return;

            int score = cell.getMiningScore();
            if (score < cfg.miningThreshold) return;

            RandomSource random = serverWorld.getRandom();
            double chance = collapseChance(score);

            if (random.nextDouble() > chance) return;

            if (hasStabilizerNearby(serverWorld, pos)) {
                serverWorld.playSound(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SoundEvents.CREAKING_AMBIENT,
                        SoundSource.BLOCKS,
                        0.6f,
                        0.2f
                );

                cell.setLastMiningActivityTick(now);
                stateData.setDirty();
                return;
            }

            if (tryProtectCollapseWithLamp(serverWorld, player, pos)) {
                cell.setLastMiningActivityTick(now);
                stateData.setDirty();
                return;
            }

            serverWorld.playSound(
                    null,
                    pos,
                    SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.BLOCKS,
                    1.2f,
                    0.5f
            );

            serverWorld.playSound(
                    null,
                    pos,
                    SoundEvents.GRAVEL_BREAK,
                    SoundSource.BLOCKS,
                    1.0f,
                    0.5f
            );

            var particleEffect = new BlockParticleOption(
                    ParticleTypes.FALLING_DUST,
                    Blocks.GRAVEL.defaultBlockState()
            );

            serverWorld.sendParticles(
                    particleEffect,
                    pos.getX() + 0.5,
                    pos.getY() + 2.5,
                    pos.getZ() + 0.5,
                    25,
                    1.0,
                    0.2,
                    1.0,
                    0.05
            );

            serverWorld.getServer().execute(() -> {
                triggerCollapse(serverWorld, player, pos);

                if (random.nextFloat() < cfg.collapseMobSpawnChance) {
                    trySpawnCollapseMob(serverWorld, player, pos);
                }
            });

            cell.setLastMiningActivityTick(now);
            stateData.setDirty();
        });
    }

    private static boolean tryProtectCollapseWithLamp(ServerLevel world, Player player, BlockPos pos) {
        if (!hasActiveWardingLantern(player)) {
            return false;
        }

        world.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.LANTERN_PLACE,
                SoundSource.BLOCKS,
                1.0f,
                0.6f
        );

        world.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS,
                0.8f,
                1.4f
        );

        world.sendParticles(
                ParticleTypes.SOUL,
                pos.getX() + 0.5,
                pos.getY() + 1.2,
                pos.getZ() + 0.5,
                18,
                0.8,
                0.5,
                0.8,
                0.04
        );

        world.sendParticles(
                ParticleTypes.END_ROD,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                10,
                0.35,
                0.45,
                0.35,
                0.03
        );

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.translatable("eroded.lamp_blocked_collapse")
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
        }

        return true;
    }

    private static boolean hasActiveWardingLantern(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return mainHand.is(ErodedBlocks.WARDING_LANTERN.asItem())
                || offHand.is(ErodedBlocks.WARDING_LANTERN.asItem());
    }

    private static void handleMiningEnergy(Player player, BlockState state) {
        SkillData energyData = SkillManager.get((ServerPlayer) player);

        var energyCfg = EnergyConfig.get().server;
        var cost = calculateEnergyCost(player, state, energyCfg);

        if (energyCfg.core.blockWorkAtZero && energyData.getEnergy() <= 0) {
            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.MINING_FATIGUE,
                            80,
                            4,
                            true,
                            false
                    )
            );
            return;
        }

        energyData.consumeEnergy(cost);

        if (energyCfg.fatigueWhenExhausted) {
            SkillData.EnergyState currentState = energyData.getEnergyState();

            if (SkillData.severity(currentState) >= SkillData.severity(SkillData.EnergyState.EXHAUSTED)) {
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.MINING_FATIGUE,
                                100,
                                2,
                                true,
                                false
                        )
                );
            }
        }
    }

    private static int calculateEnergyCost(Player player, BlockState state, EnergyConfig.Server cfg) {
        ItemStack stack = player.getMainHandItem();
        RandomSource random = player.level().getRandom();

        float base = cfg.core.miningCost;
        float chance;

        if (stack.isEmpty()) {
            chance = base * 3.0f;
        } else {
            boolean isCorrectTool = stack.isCorrectToolForDrops(state);
            float speed = stack.getDestroySpeed(state);

            if (!isCorrectTool) {
                chance = base * 3.0f;
            } else if (speed <= 2.0f) {
                chance = base * 2.0f;
            } else {
                chance = base;
            }
        }

        if (random.nextFloat() < chance) {
            return 1;
        }

        return 0;
    }

    private static double collapseChance(int miningScore) {
        var cfg = TerritoryConfig.get().server;

        if (miningScore < 500) return cfg.collapseChanceLow;
        if (miningScore < 1000) return cfg.collapseChanceMid;

        return cfg.collapseChanceHigh;
    }

    private static boolean hasStabilizerNearby(ServerLevel world, BlockPos origin) {
        var cfg = TerritoryConfig.get().server;
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();

        int radius = Math.min(cfg.stabilizerRadius, 8);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
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
        var stabilizerTag = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.BLOCK,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eroded", "stabilizers")
        );

        return state.is(stabilizerTag);
    }

    private static void triggerCollapse(ServerLevel world, Player player, BlockPos origin) {
        float pitch = player.getXRot();
        RandomSource random = world.getRandom();

        if (Math.abs(pitch) > 45) {
            Direction fillDir = (pitch > 45) ? Direction.UP : Direction.DOWN;
            BlockPos explosionPos = origin.relative(fillDir, 1);

            world.explode(
                    null,
                    explosionPos.getX() + 0.5,
                    explosionPos.getY() + 0.5,
                    explosionPos.getZ() + 0.5,
                    2.5f,
                    false,
                    Level.ExplosionInteraction.NONE
            );

            int length = 6;

            for (int i = 0; i < length; i++) {
                BlockPos layer = explosionPos.relative(fillDir, i);

                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos target = layer.offset(x, 0, z);

                        if (world.isEmptyBlock(target) || isCollapsable(world.getBlockState(target))) {
                            world.setBlock(target, Blocks.GRAVEL.defaultBlockState(), 3);
                        }
                    }
                }
            }
        } else {
            Direction behind = player.getDirection().getOpposite();

            world.playSound(
                    null,
                    origin.getX() + 0.5,
                    origin.getY() + 0.5,
                    origin.getZ() + 0.5,
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS,
                    0.8f,
                    0.5f
            );

            int depth = 6;
            int startOffset = 5;

            for (int d = startOffset; d < startOffset + depth; d++) {
                BlockPos center = origin.relative(behind, d);

                for (int w = -2; w <= 2; w++) {
                    for (int h = 1; h <= 4; h++) {
                        BlockPos target = center.above(h);

                        if (behind.getAxis() == Direction.Axis.X) {
                            target = target.offset(0, 0, w);
                        } else {
                            target = target.offset(w, 0, 0);
                        }

                        BlockState targetState = world.getBlockState(target);

                        if (isCollapsable(targetState)) {
                            world.setBlock(target, Blocks.GRAVEL.defaultBlockState(), 3);

                            if (random.nextInt(3) == 0) {
                                world.sendParticles(
                                        new BlockParticleOption(
                                                ParticleTypes.BLOCK,
                                                Blocks.GRAVEL.defaultBlockState()
                                        ),
                                        target.getX() + 0.5,
                                        target.getY() + 0.5,
                                        target.getZ() + 0.5,
                                        3,
                                        0.2,
                                        0.2,
                                        0.2,
                                        0.05
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isCollapsable(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE);
    }

    private static void trySpawnCollapseMob(ServerLevel world, Player player, BlockPos origin) {
        RandomSource random = world.getRandom();

        Direction behind = player.getDirection().getOpposite();

        int collapseStartOffset = 5;
        int collapseDepth = 6;
        int safeBufferBehindCollapse = 2;

        int minDistance = collapseStartOffset + collapseDepth + safeBufferBehindCollapse;
        int maxExtraDistance = 6;

        for (int attempt = 0; attempt < 8; attempt++) {
            int distance = minDistance + random.nextInt(maxExtraDistance + 1);

            BlockPos basePos = origin.relative(behind, distance);

            BlockPos finalPos = findSafeMobSpawnPos(world, basePos);

            if (finalPos == null) {
                continue;
            }

            double distanceSqToPlayer = player.distanceToSqr(
                    finalPos.getX() + 0.5,
                    finalPos.getY(),
                    finalPos.getZ() + 0.5
            );

            if (distanceSqToPlayer < 100.0) {
                continue;
            }

            EntityType<? extends Monster> type =
                    COLLAPSE_MOBS.get(random.nextInt(COLLAPSE_MOBS.size()));

            Monster mob = type.create(world, EntitySpawnReason.EVENT);

            if (mob == null) {
                continue;
            }

            mob.snapTo(
                    finalPos.getX() + 0.5,
                    finalPos.getY(),
                    finalPos.getZ() + 0.5,
                    random.nextFloat() * 360.0f,
                    0.0f
            );

            mob.addTag(MutatedMobResolver.MUTATED_TAG);
            mob.addTag("eroded_special_mob");

            world.addFreshEntityWithPassengers(mob);

            world.sendParticles(
                    ParticleTypes.SMOKE,
                    finalPos.getX() + 0.5,
                    finalPos.getY() + 1.0,
                    finalPos.getZ() + 0.5,
                    12,
                    0.35,
                    0.45,
                    0.35,
                    0.03
            );

            world.playSound(
                    null,
                    finalPos.getX() + 0.5,
                    finalPos.getY() + 0.5,
                    finalPos.getZ() + 0.5,
                    SoundEvents.GRAVEL_BREAK,
                    SoundSource.HOSTILE,
                    0.8f,
                    0.6f
            );

            return;
        }
    }
    private static BlockPos findSafeMobSpawnPos(ServerLevel world, BlockPos basePos) {
        for (int y = -4; y <= 4; y++) {
            BlockPos check = basePos.above(y);

            boolean feetFree = world.isEmptyBlock(check);
            boolean headFree = world.isEmptyBlock(check.above());
            boolean groundSolid = world.getBlockState(check.below()).isRedstoneConductor(world, check.below());

            if (feetFree && headFree && groundSolid) {
                return check;
            }
        }

        return null;
    }
}
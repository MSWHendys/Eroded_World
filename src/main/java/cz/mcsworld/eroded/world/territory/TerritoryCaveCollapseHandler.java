package cz.mcsworld.eroded.world.territory;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import cz.mcsworld.eroded.world.darkness.MutatedMobResolver;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;


import java.util.List;

public final class TerritoryCaveCollapseHandler {

    private TerritoryCaveCollapseHandler() {}

    private static final List<EntityType<? extends HostileEntity>> COLLAPSE_MOBS =
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
            if (!(world instanceof ServerWorld serverWorld)) return;

            if (!player.isCreative()) {
                handleMiningEnergy(player, state);
            }

            if (pos.getY() > cfg.collapseMaxY) return;

            ChunkPos chunk = new ChunkPos(pos);
            TerritoryCellKey key = TerritoryCellKey.fromChunk(chunk.x, chunk.z);

            TerritoryWorldState stateData = TerritoryWorldState.get(serverWorld);
            TerritoryCell cell = stateData.getOrCreateCell(key);

            long now = serverWorld.getTime();
            long cooldownTicks = cfg.collapseCooldownMs / 50;
            if (now - cell.getLastMiningActivityTick() < cooldownTicks) return;

            int score = cell.getMiningScore();
            if (score < cfg.miningThreshold) return;

            Random random = serverWorld.getRandom();
            double chance = collapseChance(score);
            if (random.nextDouble() > chance) return;

            if (hasStabilizerNearby(serverWorld, pos)) {
                serverWorld.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ENTITY_CREAKING_AMBIENT, SoundCategory.BLOCKS, 0.6f, 0.2f);
                return;
            }

            serverWorld.playSound(null, pos, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.BLOCKS, 1.2f, 0.5f);
            serverWorld.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1.0f, 0.5f);

            var particleEffect = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.getDefaultState());
            serverWorld.spawnParticles(particleEffect, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5,
                    25, 1.0, 0.2, 1.0, 0.05);

            serverWorld.getServer().execute(() -> {
                triggerCollapse(serverWorld, player, pos);
                if (random.nextFloat() < cfg.collapseMobSpawnChance) {
                    trySpawnCollapseMob(serverWorld, player, pos);
                }
            });

            cell.setLastMiningActivityTick(now);
            stateData.markDirty();
        });
    }

    private static void handleMiningEnergy(PlayerEntity player, BlockState state) {

        SkillData energyData = SkillManager.get((ServerPlayerEntity) player);

        var energyCfg = EnergyConfig.get().server;

        var cost = calculateEnergyCost(player, state, energyCfg);

        if (energyCfg.core.blockWorkAtZero && energyData.getEnergy() <= 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 80, 4, true, false));
            return;
        }

        energyData.consumeEnergy(cost);

        if (energyCfg.fatigueWhenExhausted) {
            SkillData.EnergyState currentState = energyData.getEnergyState();
            if (SkillData.severity(currentState) >= SkillData.severity(SkillData.EnergyState.EXHAUSTED)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 100, 2, true, false));
            }
        }
    }

    private static int calculateEnergyCost(PlayerEntity player, BlockState state, EnergyConfig.Server cfg) {
        ItemStack stack = player.getMainHandStack();
        Random random = player.getWorld().getRandom();

        float base = cfg.core.miningCost;
        float chance;

        if (stack.isEmpty()) {
            chance = base * 3.0f;
        } else {
            boolean isCorrectTool = stack.isSuitableFor(state);
            float speed = stack.getMiningSpeedMultiplier(state);

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

    private static boolean hasStabilizerNearby(ServerWorld world, BlockPos origin) {
        var cfg = TerritoryConfig.get().server;
        BlockPos.Mutable check = new BlockPos.Mutable();
        int radius = Math.min(cfg.stabilizerRadius, 8);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    check.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (isStabilizer(world.getBlockState(check))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isStabilizer(BlockState state) {
        var stabilizerTag = net.minecraft.registry.tag.TagKey.of(
                net.minecraft.registry.RegistryKeys.BLOCK,
                net.minecraft.util.Identifier.of("eroded", "stabilizers")
        );
        return state.isIn(stabilizerTag);
    }

    private static void triggerCollapse(ServerWorld world, PlayerEntity player, BlockPos origin) {
        float pitch = player.getPitch();
        Random random = world.getRandom();

        if (Math.abs(pitch) > 45) {
            Direction fillDir = (pitch > 45) ? Direction.UP : Direction.DOWN;
            BlockPos explosionPos = origin.offset(fillDir, 1);

            world.createExplosion(null,
                    explosionPos.getX() + 0.5, explosionPos.getY() + 0.5, explosionPos.getZ() + 0.5,
                    2.5f, false, ServerWorld.ExplosionSourceType.NONE);

            int length = 6;
            for (int i = 0; i < length; i++) {
                BlockPos layer = explosionPos.offset(fillDir, i);
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos target = layer.add(x, 0, z);
                        if (world.isAir(target) || isCollapsable(world.getBlockState(target))) {
                            world.setBlockState(target, Blocks.GRAVEL.getDefaultState(), 3);
                        }
                    }
                }
            }
        } else {
            Direction behind = player.getHorizontalFacing().getOpposite();

            world.playSound(null, origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.8f, 0.5f);

            int depth = 6;
            int startOffset = 5;

            for (int d = startOffset; d < startOffset + depth; d++) {
                BlockPos center = origin.offset(behind, d);
                for (int w = -2; w <= 2; w++) {
                    for (int h = 1; h <= 4; h++) {
                        BlockPos target = center.up(h);
                        if (behind.getAxis() == Direction.Axis.X) target = target.add(0, 0, w);
                        else target = target.add(w, 0, 0);

                        BlockState state = world.getBlockState(target);
                        if (isCollapsable(state)) {
                            world.setBlockState(target, Blocks.GRAVEL.getDefaultState(), 3);
                            if (random.nextInt(3) == 0) {
                                world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.GRAVEL.getDefaultState()),
                                        target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.05);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isCollapsable(BlockState state) {
        return state.isOf(Blocks.STONE) || state.isOf(Blocks.DEEPSLATE) || state.isOf(Blocks.TUFF)
                || state.isOf(Blocks.ANDESITE) || state.isOf(Blocks.DIORITE) || state.isOf(Blocks.GRANITE);
    }

    private static void trySpawnCollapseMob(ServerWorld world, PlayerEntity player, BlockPos origin) {
        Random random = world.getRandom();
        Direction behind = player.getHorizontalFacing().getOpposite();

        for (int i = 0; i < 3; i++) {
            BlockPos spawnPos = origin.offset(behind, 2 + random.nextInt(3));
            BlockPos finalPos = null;
            for (int y = -3; y <= 3; y++) {
                BlockPos check = spawnPos.up(y);
                if (world.isAir(check) && world.isAir(check.up()) && world.getBlockState(check.down()).isSolidBlock(world, check.down())) {
                    finalPos = check;
                    break;
                }
            }

            if (finalPos != null) {
                EntityType<? extends HostileEntity> type = (random.nextBoolean()) ? EntityType.ZOMBIE : EntityType.SKELETON;
                HostileEntity mob = type.create(world, SpawnReason.EVENT);
                if (mob != null) {
                    mob.refreshPositionAndAngles(finalPos.getX() + 0.5, finalPos.getY(), finalPos.getZ() + 0.5, random.nextFloat() * 360, 0);
                    mob.addCommandTag(MutatedMobResolver.MUTATED_TAG);
                    mob.addCommandTag("eroded_special_mob");
                    world.spawnEntityAndPassengers(mob);
                    return;
                }
            }
        }
    }
}
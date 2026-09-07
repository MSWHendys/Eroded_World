package cz.mcsworld.eroded.world.territory;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.world.darkness.MutatedMobResolver;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class TerritoryCaveCollapseHandler {

    private TerritoryCaveCollapseHandler() {
    }

    private static final List<EntityType<? extends @NotNull Monster>> COLLAPSE_MOBS =
            List.of(
                    EntityTypes.ZOMBIE,
                    EntityTypes.SKELETON,
                    EntityTypes.SPIDER,
                    EntityTypes.CREEPER
            );

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {

            var cfg = TerritoryConfig.get().server;
            if (!cfg.enabled || !cfg.caveCollapseEnabled) return;
            if (!(world instanceof ServerLevel serverWorld)) return;

            if (pos.getY() > cfg.collapseMaxY) return;

            ChunkPos chunk = new ChunkPos(
                    player.blockPosition().getX() >> 4,
                    player.blockPosition().getZ() >> 4);
            TerritoryCellKey key = TerritoryCellKey.fromChunk(chunk.x(), chunk.z());

            TerritoryWorldState stateData = TerritoryWorldState.get(serverWorld);
            TerritoryCell cell = stateData.getOrCreateCell(key);

            long now = serverWorld.getGameTime();
            long cooldownTicks = cfg.collapseCooldownMs / 50;

            long lastCollapseTick = cell.getLastCollapseTick();
            if (lastCollapseTick > 0L && now >= lastCollapseTick
                    && now - lastCollapseTick < cooldownTicks) return;

            int score = cell.getMiningScore();
            if (score < cfg.miningThreshold) return;

            RandomSource random = serverWorld.getRandom();
            double chance = collapseChance(score);

            if (random.nextDouble() > chance) return;

            if (hasOverheadStabilizer(serverWorld, player)) {
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

                cell.setLastCollapseTick(now);
                stateData.setDirty();
                return;
            }

            if (tryProtectCollapseWithLamp(serverWorld, player, pos)) {
                cell.setLastCollapseTick(now);
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
                triggerCollapse(serverWorld, player);

                if (random.nextFloat() < cfg.collapseMobSpawnChance) {
                    trySpawnCollapseMob(serverWorld, player);
                }
            });

            cell.setLastCollapseTick(now);
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
            serverPlayer.sendSystemMessage(
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

    private static double collapseChance(int miningScore) {
        var cfg = TerritoryConfig.get().server;

        if (miningScore < 500) return cfg.collapseChanceLow;
        if (miningScore < 1000) return cfg.collapseChanceMid;

        return cfg.collapseChanceHigh;
    }

    /**
     * A tunnel stabilizer only protects the player when it is actually installed
     * overhead.  The old implementation scanned a full cube around the mined
     * block, which meant that a log lying on the floor (or even below the player)
     * prevented collapses as well.
     *
     * The configured stabilizer radius remains the horizontal protection radius.
     * Vertically we only inspect the normal ceiling band: two to four blocks above
     * the player's feet.  This covers ordinary 2-block tunnels as well as slightly
     * taller mine passages without allowing floor/wall supports to count.
     */
    private static boolean hasOverheadStabilizer(ServerLevel world, Player player) {
        var cfg = TerritoryConfig.get().server;
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();

        int radius = Math.min(cfg.stabilizerRadius, 8);
        int radiusSquared = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSquared) continue;

                for (int dy = 2; dy <= 4; dy++) {
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
                net.minecraft.resources.Identifier.fromNamespaceAndPath("eroded", "stabilizers")
        );

        return state.is(stabilizerTag);
    }

    private record HorizontalFrame(double backX, double backZ, double rightX, double rightZ) {
    }

    /**
     * Builds an exact horizontal frame from the player's look vector.  Using the
     * look vector (rather than the four-way getDirection()) keeps diagonal
     * tunnels from rotating the collapse band partly in front of the player.
     */
    private static HorizontalFrame horizontalFrame(Player player) {
        Vec3 look = player.getLookAngle();
        double length = Math.sqrt(look.x * look.x + look.z * look.z);

        double forwardX;
        double forwardZ;

        if (length < 1.0E-4) {
            var facing = player.getDirection();
            forwardX = facing.getStepX();
            forwardZ = facing.getStepZ();
        } else {
            forwardX = look.x / length;
            forwardZ = look.z / length;
        }

        // Back is exactly opposite the horizontal look direction.
        double backX = -forwardX;
        double backZ = -forwardZ;

        // Right is perpendicular to forward on the X/Z plane.
        double rightX = -forwardZ;
        double rightZ = forwardX;

        return new HorizontalFrame(backX, backZ, rightX, rightZ);
    }

    private static BlockPos behindPlayer(Player player, HorizontalFrame frame, double backDistance, double sideOffset, int yOffset) {
        return BlockPos.containing(
                player.getX() + frame.backX() * backDistance + frame.rightX() * sideOffset,
                player.getY() + yOffset,
                player.getZ() + frame.backZ() * backDistance + frame.rightZ() * sideOffset
        );
    }

    private static void triggerCollapse(ServerLevel world, Player player) {
        float pitch = player.getXRot();

        // Looking distinctly down means the player is most likely digging a
        // shaft below themselves.  In that case the danger must come from the
        // ceiling above the player, never from the broken block below/forward.
        if (pitch > 45.0f) {
            triggerOverheadCollapse(world, player);
            return;
        }

        // Horizontal mining and upward mining both use a rear cave-in.
        triggerRearCollapse(world, player);
    }

    private static void triggerOverheadCollapse(ServerLevel world, Player player) {
        RandomSource random = world.getRandom();
        BlockPos playerPos = player.blockPosition();
        BlockPos effectPos = playerPos.above(3);

        world.explode(
                null,
                effectPos.getX() + 0.5,
                effectPos.getY() + 0.5,
                effectPos.getZ() + 0.5,
                2.5f,
                false,
                Level.ExplosionInteraction.NONE
        );

        // A compact ceiling cap starts safely above the player's body.  Gravel
        // may replace a ceiling block or be created in air, but never starts at
        // the player's feet as the old vertical column did.
        for (int layer = 0; layer < 2; layer++) {
            int y = 3 + layer;

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos target = playerPos.offset(x, y, z);
                    BlockState state = world.getBlockState(target);

                    if (!world.isEmptyBlock(target) && !isCollapsable(state)) {
                        continue;
                    }

                    world.setBlock(target, Blocks.GRAVEL.defaultBlockState(), 3);
                    maybeCollapseParticles(world, random, target);
                }
            }
        }
    }

    private static void triggerRearCollapse(ServerLevel world, Player player) {
        RandomSource random = world.getRandom();
        HorizontalFrame frame = horizontalFrame(player);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                0.8f,
                0.5f
        );

        // Start behind the player, not behind the block that was just mined.
        // This guarantees that even a close/sideways mined origin cannot move
        // the collapse band in front of the player.
        int startBehindPlayer = 3;
        int depth = 6;

        for (int d = startBehindPlayer; d < startBehindPlayer + depth; d++) {
            for (int w = -2; w <= 2; w++) {
                // Search upward from the tunnel head space and destabilise the
                // first two collapsable ceiling blocks in this rear column.
                int converted = 0;

                for (int h = 2; h <= 6 && converted < 2; h++) {
                    BlockPos target = behindPlayer(player, frame, d, w, h);
                    BlockState targetState = world.getBlockState(target);

                    if (!isCollapsable(targetState)) {
                        continue;
                    }

                    world.setBlock(target, Blocks.GRAVEL.defaultBlockState(), 3);
                    converted++;
                    maybeCollapseParticles(world, random, target);
                }
            }
        }
    }

    private static void maybeCollapseParticles(ServerLevel world, RandomSource random, BlockPos target) {
        if (random.nextInt(3) != 0) {
            return;
        }

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

    private static boolean isCollapsable(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE);
    }

    private static void trySpawnCollapseMob(ServerLevel world, Player player) {
        RandomSource random = world.getRandom();

        HorizontalFrame frame = horizontalFrame(player);

        int collapseStartOffset = 3;
        int collapseDepth = 6;
        int safeBufferBehindCollapse = 2;

        int minDistance = collapseStartOffset + collapseDepth + safeBufferBehindCollapse;
        int maxExtraDistance = 6;

        for (int attempt = 0; attempt < 8; attempt++) {
            int distance = minDistance + random.nextInt(maxExtraDistance + 1);

            BlockPos basePos = behindPlayer(player, frame, distance, 0.0, 0);

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

            EntityType<? extends @NotNull Monster> type =
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

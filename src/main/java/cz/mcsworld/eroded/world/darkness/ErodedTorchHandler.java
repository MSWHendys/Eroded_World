package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.item.ErodedTorchItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.LightType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ErodedTorchHandler {

    private static final Map<UUID, LastLight> LAST_LIGHT =
            new ConcurrentHashMap<>();

    private ErodedTorchHandler() {
    }

    private record LastLight(
            RegistryKey<World> worldKey,
            BlockPos pos
    ) {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ErodedTorchHandler::onTick);
    }

    private static void onTick(MinecraftServer server) {
        var root = DarknessConfigs.get();
        var cfg = root.server.erodedTorch;

        boolean systemEnabled = root.enabled && cfg.enabled;

        int rechargeInterval = Math.max(
                1,
                cfg.rechargeIntervalTicks
        );

        boolean rechargeTick =
                server.getTicks() % rechargeInterval == 0;

        boolean secondTick =
                server.getTicks() % 20 == 0;

        for (ServerPlayerEntity player :
                server.getPlayerManager().getPlayerList()) {

            tickPlayer(
                    server,
                    player,
                    systemEnabled,
                    rechargeTick,
                    secondTick
            );
        }
    }

    private static void tickPlayer(
            MinecraftServer server,
            ServerPlayerEntity player,
            boolean systemEnabled,
            boolean rechargeTick,
            boolean secondTick
    ) {
        var cfg = DarknessConfigs.get().server.erodedTorch;

        ServerWorld world = player.getWorld();
        UUID uuid = player.getUuid();

        ItemStack torch = getHeldTorch(player);
        boolean held = !torch.isEmpty();

        if (rechargeTick) {
            rechargeInventory(player);
        }

        if (!systemEnabled || !held) {
            cleanup(uuid, server);
            return;
        }

        ensureTorchMaxDamage(torch);

        boolean environmentActive =
                !cfg.drainOnlyInDarkness
                        || isTorchEnvironmentActive(
                        world,
                        player.getBlockPos()
                );

        if (!environmentActive) {
            cleanup(uuid, server);

            if (rechargeTick && cfg.rechargeHeldWhenInactive) {
                boolean recharged = rechargeStack(torch);

                if (recharged) {
                    player.getInventory().markDirty();
                }
            }

            return;
        }

        boolean active = ErodedTorchItem.hasCharge(torch);

        if (!active) {
            cleanup(uuid, server);
            return;
        }

        if (secondTick) {
            ErodedTorchItem.drain(torch);

            if (!ErodedTorchItem.hasCharge(torch)) {
                cleanup(uuid, server);
                return;
            }
        }

        BlockPos playerPos = player.getBlockPos();
        BlockPos wantedPos = findLightPos(world, playerPos);

        if (wantedPos == null) {
            cleanup(uuid, server);
            return;
        }

        LastLight oldLight = LAST_LIGHT.get(uuid);

        if (oldLight != null) {
            if (oldLight.worldKey().equals(world.getRegistryKey())) {
                BlockPos oldPos = oldLight.pos();

                if (player.squaredDistanceTo(
                        oldPos.getX() + 0.5D,
                        oldPos.getY() + 0.5D,
                        oldPos.getZ() + 0.5D
                ) < 2.25D) {

                    if (world.getBlockState(oldPos).isOf(Blocks.LIGHT)) {
                        return;
                    }

                    if (world.isAir(oldPos)) {
                        placeLightAt(world, oldPos);
                        return;
                    }
                }
            }

            cleanup(uuid, server);
        }

        if (canPlaceLightAt(world, wantedPos)) {
            placeLightAt(world, wantedPos);

            LAST_LIGHT.put(
                    uuid,
                    new LastLight(
                            world.getRegistryKey(),
                            wantedPos.toImmutable()
                    )
            );
        }
    }
    private static boolean isTorchEnvironmentActive(
            ServerWorld world,
            BlockPos pos
    ) {
        var cfg = DarknessConfigs.get().server.erodedTorch;

        long time = world.getTimeOfDay() % 24000L;

        boolean eveningOrNight = isTimeInside(
                time,
                cfg.activeNightStartTime,
                cfg.activeNightEndTime
        );

        int skyLight = world.getLightLevel(
                LightType.SKY,
                pos
        );

        boolean shadowOrCover =
                !world.isSkyVisible(pos)
                        || skyLight <= cfg.activeSkyLightMax;

        return eveningOrNight || shadowOrCover;
    }

    private static boolean isTimeInside(
            long time,
            int start,
            int end
    ) {
        int safeStart = MathHelper.clamp(
                start,
                0,
                23999
        );

        int safeEnd = MathHelper.clamp(
                end,
                0,
                23999
        );

        if (safeStart <= safeEnd) {
            return time >= safeStart && time <= safeEnd;
        }

        return time >= safeStart || time <= safeEnd;
    }

    private static boolean rechargeStack(ItemStack stack) {
        var cfg = DarknessConfigs.get().server.erodedTorch;

        if (!isErodedTorch(stack)) {
            return false;
        }

        ensureTorchMaxDamage(stack);

        if (ErodedTorchItem.isFullyCharged(stack)) {
            return false;
        }

        int rechargeAmount = Math.max(
                0,
                cfg.rechargeAmount
        );

        if (rechargeAmount < 1) {
            return false;
        }

        ErodedTorchItem.recharge(
                stack,
                rechargeAmount
        );

        return true;
    }


    private static void ensureTorchMaxDamage(ItemStack torch) {
        int targetMax = ErodedTorchItem.getTargetMaxDamage();

        if (torch.getMaxDamage() != targetMax) {
            torch.set(DataComponentTypes.MAX_DAMAGE, targetMax);

            if (torch.getDamage() >= targetMax) {
                torch.setDamage(targetMax);
            }
        }
    }

    private static ItemStack getHeldTorch(ServerPlayerEntity player) {
        if (isErodedTorch(player.getMainHandStack())) {
            return player.getMainHandStack();
        }

        if (isErodedTorch(player.getOffHandStack())) {
            return player.getOffHandStack();
        }

        return ItemStack.EMPTY;
    }

    private static boolean isErodedTorch(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isOf(ErodedBlocks.ERODED_TORCH_ITEM);
    }

    private static void rechargeInventory(ServerPlayerEntity player) {
        var cfg = DarknessConfigs.get().server.erodedTorch;

        int rechargeAmount = Math.max(
                0,
                cfg.rechargeAmount
        );

        if (rechargeAmount < 1) {
            return;
        }

        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);

            if (!isErodedTorch(stack)) {
                continue;
            }

            if (isHeldStack(player, stack)) {
                continue;
            }

            ensureTorchMaxDamage(stack);

            if (ErodedTorchItem.isFullyCharged(stack)) {
                continue;
            }

            ErodedTorchItem.recharge(
                    stack,
                    rechargeAmount
            );
        }
    }

    private static boolean isHeldStack(
            ServerPlayerEntity player,
            ItemStack stack
    ) {
        return stack == player.getMainHandStack()
                || stack == player.getOffHandStack();
    }

    private static BlockPos findLightPos(
            ServerWorld world,
            BlockPos playerPos
    ) {
        if (canPlaceLightAt(world, playerPos.up())) {
            return playerPos.up().toImmutable();
        }

        if (canPlaceLightAt(world, playerPos)) {
            return playerPos.toImmutable();
        }

        return null;
    }

    private static boolean canPlaceLightAt(
            ServerWorld world,
            BlockPos pos
    ) {
        return world.isAir(pos)
                || world.getBlockState(pos).isOf(Blocks.LIGHT);
    }

    private static void placeLightAt(
            ServerWorld world,
            BlockPos pos
    ) {
        if (!canPlaceLightAt(world, pos)) {
            return;
        }

        int lightLevel = MathHelper.clamp(
                DarknessConfigs.get()
                        .server
                        .erodedTorch
                        .placedLightLevel,
                1,
                15
        );

        world.setBlockState(
                pos,
                Blocks.LIGHT
                        .getDefaultState()
                        .with(LightBlock.LEVEL_15, lightLevel),
                Block.NOTIFY_ALL
        );
    }

    private static void removeLightAt(
            ServerWorld world,
            BlockPos pos
    ) {
        if (world != null
                && world.getBlockState(pos).isOf(Blocks.LIGHT)) {

            world.setBlockState(
                    pos,
                    Blocks.AIR.getDefaultState(),
                    Block.NOTIFY_ALL
            );
        }
    }

    public static void cleanup(
            UUID uuid,
            MinecraftServer server
    ) {
        LastLight lastLight = LAST_LIGHT.remove(uuid);

        if (lastLight == null) {
            return;
        }

        ServerWorld world = server.getWorld(lastLight.worldKey());

        if (world != null) {
            removeLightAt(
                    world,
                    lastLight.pos()
            );
        }
    }

    public static void cleanup(
            UUID uuid,
            ServerWorld fallbackWorld
    ) {
        LastLight lastLight = LAST_LIGHT.remove(uuid);

        if (lastLight != null
                && fallbackWorld.getRegistryKey().equals(lastLight.worldKey())) {

            removeLightAt(
                    fallbackWorld,
                    lastLight.pos()
            );
        }
    }
}
package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.item.ErodedTorchItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ErodedTorchHandler {

    private static final Map<UUID, LastLight> LAST_LIGHT =
            new ConcurrentHashMap<>();

    private ErodedTorchHandler() {
    }

    private record LastLight(
            ResourceKey<Level> worldKey,
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
                server.getTickCount() % rechargeInterval == 0;

        boolean secondTick =
                server.getTickCount() % 20 == 0;

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {

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
            ServerPlayer player,
            boolean systemEnabled,
            boolean rechargeTick,
            boolean secondTick
    ) {
        var cfg = DarknessConfigs.get().server.erodedTorch;

        ServerLevel world = player.level();
        UUID uuid = player.getUUID();

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
                        player.blockPosition()
                );

        if (!environmentActive) {
            cleanup(uuid, server);

            if (rechargeTick && cfg.rechargeHeldWhenInactive) {
                boolean recharged = rechargeStack(torch);

                if (recharged) {
                    player.getInventory().setChanged();
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

        BlockPos playerPos = player.blockPosition();
        BlockPos wantedPos = findLightPos(world, playerPos);

        if (wantedPos == null) {
            cleanup(uuid, server);
            return;
        }

        LastLight oldLight = LAST_LIGHT.get(uuid);

        if (oldLight != null) {
            if (oldLight.worldKey().equals(world.dimension())) {
                BlockPos oldPos = oldLight.pos();

                if (player.distanceToSqr(
                        oldPos.getX() + 0.5D,
                        oldPos.getY() + 0.5D,
                        oldPos.getZ() + 0.5D
                ) < 2.25D) {

                    if (world.getBlockState(oldPos).is(Blocks.LIGHT)) {
                        return;
                    }

                    if (world.isEmptyBlock(oldPos)) {
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
                            world.dimension(),
                            wantedPos.immutable()
                    )
            );
        }
    }
    private static boolean isTorchEnvironmentActive(
            ServerLevel world,
            BlockPos pos
    ) {
        var cfg = DarknessConfigs.get().server.erodedTorch;

        long time = world.getDayTime() % 24000L;

        boolean eveningOrNight = isTimeInside(
                time,
                cfg.activeNightStartTime,
                cfg.activeNightEndTime
        );

        int skyLight = world.getBrightness(
                LightLayer.SKY,
                pos
        );

        boolean shadowOrCover =
                !world.canSeeSky(pos)
                        || skyLight <= cfg.activeSkyLightMax;

        return eveningOrNight || shadowOrCover;
    }

    private static boolean isTimeInside(
            long time,
            int start,
            int end
    ) {
        int safeStart = Mth.clamp(
                start,
                0,
                23999
        );

        int safeEnd = Mth.clamp(
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
            torch.set(DataComponents.MAX_DAMAGE, targetMax);

            if (torch.getDamageValue() >= targetMax) {
                torch.setDamageValue(targetMax);
            }
        }
    }

    private static ItemStack getHeldTorch(ServerPlayer player) {
        if (isErodedTorch(player.getMainHandItem())) {
            return player.getMainHandItem();
        }

        if (isErodedTorch(player.getOffhandItem())) {
            return player.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    private static boolean isErodedTorch(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(ErodedBlocks.ERODED_TORCH_ITEM);
    }

    private static void rechargeInventory(ServerPlayer player) {
        var cfg = DarknessConfigs.get().server.erodedTorch;

        int rechargeAmount = Math.max(
                0,
                cfg.rechargeAmount
        );

        if (rechargeAmount < 1) {
            return;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);

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
            ServerPlayer player,
            ItemStack stack
    ) {
        return stack == player.getMainHandItem()
                || stack == player.getOffhandItem();
    }

    private static BlockPos findLightPos(
            ServerLevel world,
            BlockPos playerPos
    ) {
        if (canPlaceLightAt(world, playerPos.above())) {
            return playerPos.above().immutable();
        }

        if (canPlaceLightAt(world, playerPos)) {
            return playerPos.immutable();
        }

        return null;
    }

    private static boolean canPlaceLightAt(
            ServerLevel world,
            BlockPos pos
    ) {
        return world.isEmptyBlock(pos)
                || world.getBlockState(pos).is(Blocks.LIGHT);
    }

    private static void placeLightAt(
            ServerLevel world,
            BlockPos pos
    ) {
        if (!canPlaceLightAt(world, pos)) {
            return;
        }

        int lightLevel = Mth.clamp(
                DarknessConfigs.get()
                        .server
                        .erodedTorch
                        .placedLightLevel,
                1,
                15
        );

        world.setBlock(
                pos,
                Blocks.LIGHT
                        .defaultBlockState()
                        .setValue(LightBlock.LEVEL, lightLevel),
                Block.UPDATE_ALL
        );
    }

    private static void removeLightAt(
            ServerLevel world,
            BlockPos pos
    ) {
        if (world != null
                && world.getBlockState(pos).is(Blocks.LIGHT)) {

            world.setBlock(
                    pos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL
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

        ServerLevel world = server.getLevel(lastLight.worldKey());

        if (world != null) {
            removeLightAt(
                    world,
                    lastLight.pos()
            );
        }
    }

    public static void cleanup(
            UUID uuid,
            ServerLevel fallbackWorld
    ) {
        LastLight lastLight = LAST_LIGHT.remove(uuid);

        if (lastLight != null
                && fallbackWorld.dimension().equals(lastLight.worldKey())) {

            removeLightAt(
                    fallbackWorld,
                    lastLight.pos()
            );
        }
    }
}
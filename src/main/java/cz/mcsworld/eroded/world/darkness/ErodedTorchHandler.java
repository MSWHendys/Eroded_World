package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.item.ErodedTorchItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import java.util.UUID;

public final class ErodedTorchHandler {

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

        if (!systemEnabled) {
            cleanup(uuid, server);
            return;
        }

        if (rechargeTick) {
            rechargeInventory(player);
        }

        if (!held) {
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
        BlockPos wantedPos = findLightPos(world, playerPos, uuid);

        if (wantedPos == null) {
            cleanup(uuid, server);
            return;
        }

        int lightLevel = Mth.clamp(cfg.placedLightLevel, 1, 15);
        if (!DynamicLightManager.ensure(
                world,
                wantedPos,
                uuid,
                DynamicLightManager.Source.ERODED_TORCH,
                lightLevel
        )) {
            cleanup(uuid, server);
        }
    }
    private static boolean isTorchEnvironmentActive(
            ServerLevel world,
            BlockPos pos
    ) {
        var cfg = DarknessConfigs.get().server.erodedTorch;

        long time = world.getDefaultClockTime() % 24000L;

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
            BlockPos playerPos,
            UUID uuid
    ) {
        if (DynamicLightManager.canUse(
                world, playerPos.above(), uuid, DynamicLightManager.Source.ERODED_TORCH)) {
            return playerPos.above().immutable();
        }

        if (DynamicLightManager.canUse(
                world, playerPos, uuid, DynamicLightManager.Source.ERODED_TORCH)) {
            return playerPos.immutable();
        }

        return null;
    }

    public static void cleanup(
            UUID uuid,
            MinecraftServer server
    ) {
        DynamicLightManager.release(
                uuid,
                DynamicLightManager.Source.ERODED_TORCH,
                server
        );
    }

    public static void cleanup(
            UUID uuid,
            ServerLevel fallbackWorld
    ) {
        DynamicLightManager.release(
                uuid,
                DynamicLightManager.Source.ERODED_TORCH,
                fallbackWorld.getServer()
        );
    }
}

package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import java.util.UUID;

public final class ErodedLampHandler {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(server, player);
            }
        });
    }

    private static void tickPlayer(MinecraftServer server, ServerPlayer player) {
        ServerLevel world = player.level();
        UUID uuid = player.getUUID();

        ItemStack lamp = getHeldLamp(player);
        var root = DarknessConfigs.get();

        if (!root.enabled || lamp.isEmpty()) {
            cleanup(uuid, server);
            return;
        }

        var lampCfg = root.server.wardingLamp;

        ensureLampMaxDamage(lamp, lampCfg.durationSeconds);

        BlockPos playerPos = player.blockPosition();
        int skyLight = world.getBrightness(LightLayer.SKY, playerPos);

        boolean isUnderground = playerPos.getY() < lampCfg.undergroundY || !world.canSeeSky(playerPos);
        boolean isDarkEnough = skyLight <= lampCfg.skyLightMax && isUnderground;

        if (!isDarkEnough) {
            cleanup(uuid, server);
            return;
        }

        if (server.getTickCount() % 20 == 0) {
            int newDamage = lamp.getDamageValue() + 1;
            lamp.setDamageValue(newDamage);

            if (newDamage >= lamp.getMaxDamage()) {
                lamp.setCount(0);
                cleanup(uuid, server);
                return;
            }
        }

        BlockPos wantedPos = findLightPos(world, playerPos, uuid);
        if (wantedPos == null) {
            cleanup(uuid, server);
            return;
        }

        int lightLevel = Math.min(15, Math.max(1, lampCfg.lightLevel));
        if (!DynamicLightManager.ensure(
                world,
                wantedPos,
                uuid,
                DynamicLightManager.Source.WARDING_LAMP,
                lightLevel
        )) {
            cleanup(uuid, server);
        }
    }

    private static void ensureLampMaxDamage(ItemStack lamp, int durationSeconds) {
        int targetMax = Math.max(2, durationSeconds);

        if (lamp.getMaxDamage() != targetMax) {
            lamp.set(DataComponents.MAX_DAMAGE, targetMax);

            if (lamp.getDamageValue() >= targetMax) {
                lamp.setDamageValue(targetMax - 1);
            }
        }
    }

    private static BlockPos findLightPos(
            ServerLevel world,
            BlockPos playerPos,
            UUID uuid
    ) {
        if (DynamicLightManager.canUse(
                world, playerPos.above(), uuid, DynamicLightManager.Source.WARDING_LAMP)) {
            return playerPos.above().immutable();
        }
        if (DynamicLightManager.canUse(
                world, playerPos, uuid, DynamicLightManager.Source.WARDING_LAMP)) {
            return playerPos.immutable();
        }
        return null;
    }

    public static void cleanup(UUID uuid, MinecraftServer server) {
        DynamicLightManager.release(uuid, DynamicLightManager.Source.WARDING_LAMP, server);
    }

    public static void cleanup(UUID uuid, ServerLevel fallbackWorld) {
        DynamicLightManager.release(
                uuid,
                DynamicLightManager.Source.WARDING_LAMP,
                fallbackWorld.getServer()
        );
    }

    private static ItemStack getHeldLamp(ServerPlayer player) {
        if (player.getMainHandItem().is(ErodedBlocks.WARDING_LANTERN.asItem())) return player.getMainHandItem();
        if (player.getOffhandItem().is(ErodedBlocks.WARDING_LANTERN.asItem())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }
}

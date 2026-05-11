package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
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
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ErodedLampHandler {

    private static final Map<UUID, LastLight> LAST_LIGHT = new ConcurrentHashMap<>();

    private static final Map<UUID, Long> LAST_USAGE_TICK = new ConcurrentHashMap<>();

    private ErodedLampHandler() {
    }

    private record LastLight(RegistryKey<World> worldKey, BlockPos pos) {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tickPlayer(server, player);
            }
        });
    }

    private static void tickPlayer(MinecraftServer server, ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        UUID uuid = player.getUuid();

        ItemStack lamp = getHeldLamp(player);

        if (lamp.isEmpty()) {
            cleanup(uuid, server);
            return;
        }

        var lampCfg = DarknessConfigs.get().server.wardingLamp;

        if (ensureLampMaxDamage(lamp, lampCfg.durationSeconds)) {
            syncLampStack(player);
        }

        BlockPos playerPos = player.getBlockPos();

        int skyLight = world.getLightLevel(LightType.SKY, playerPos);

        boolean isUnderground =
                playerPos.getY() < lampCfg.undergroundY
                        || !world.isSkyVisible(playerPos);

        boolean isDarkEnough =
                skyLight <= lampCfg.skyLightMax
                        && isUnderground;

        if (!isDarkEnough) {
            cleanup(uuid, server);
            return;
        }

        BlockPos wantedPos = findLightPos(world, playerPos);

        if (wantedPos == null) {
            cleanup(uuid, server);
            return;
        }


        if (!tickLampUsage(server, player, uuid, lamp)) {
            cleanup(uuid, server);
            return;
        }

        LastLight oldLight = LAST_LIGHT.get(uuid);

        if (oldLight != null) {
            boolean sameWorld = oldLight.worldKey().equals(world.getRegistryKey());

            if (sameWorld) {
                BlockPos oldPos = oldLight.pos();

                boolean closeEnough = player.squaredDistanceTo(
                        oldPos.getX() + 0.5,
                        oldPos.getY() + 0.5,
                        oldPos.getZ() + 0.5
                ) < 2.25;

                if (closeEnough) {
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
            LAST_LIGHT.put(uuid, new LastLight(world.getRegistryKey(), wantedPos));
        }
    }

    private static boolean tickLampUsage(
            MinecraftServer server,
            ServerPlayerEntity player,
            UUID uuid,
            ItemStack lamp
    ) {
        long nowTick = server.getTicks();
        int maxDamage = Math.max(2, lamp.getMaxDamage());

        if (lamp.getDamage() <= 0) {
            lamp.setDamage(1);
            LAST_USAGE_TICK.put(uuid, nowTick);
            syncLampStack(player);
            return true;
        }

        Long lastTickObj = LAST_USAGE_TICK.get(uuid);

        if (lastTickObj == null) {
            LAST_USAGE_TICK.put(uuid, nowTick);
            return true;
        }

        long lastTick = lastTickObj;
        long elapsedTicks = nowTick - lastTick;

        if (elapsedTicks < 20) {
            return true;
        }

        int elapsedSeconds = (int) (elapsedTicks / 20L);

        LAST_USAGE_TICK.put(
                uuid,
                lastTick + elapsedSeconds * 20L
        );

        int newDamage = lamp.getDamage() + elapsedSeconds;

        if (newDamage >= maxDamage) {
            lamp.setCount(0);
            syncLampStack(player);
            return false;
        }

        lamp.setDamage(newDamage);
        syncLampStack(player);

        return true;
    }

    private static boolean ensureLampMaxDamage(ItemStack lamp, int durationSeconds) {

        int newMaxDamage = Math.max(2, durationSeconds + 1);
        int oldMaxDamage = Math.max(1, lamp.getMaxDamage());

        if (oldMaxDamage == newMaxDamage) {
            return false;
        }

        int oldDamage = Math.max(0, lamp.getDamage());
        float usedRatio = oldDamage / (float) oldMaxDamage;

        int newDamage = Math.round(usedRatio * newMaxDamage);
        newDamage = Math.max(0, Math.min(newMaxDamage - 1, newDamage));

        lamp.set(DataComponentTypes.MAX_DAMAGE, newMaxDamage);
        lamp.setDamage(newDamage);

        return true;
    }

    private static BlockPos findLightPos(ServerWorld world, BlockPos playerPos) {
        BlockPos abovePlayer = playerPos.up();

        if (canPlaceLightAt(world, abovePlayer)) {
            return abovePlayer;
        }

        if (canPlaceLightAt(world, playerPos)) {
            return playerPos;
        }

        return null;
    }

    private static boolean canPlaceLightAt(ServerWorld world, BlockPos pos) {
        return world.isAir(pos) || world.getBlockState(pos).isOf(Blocks.LIGHT);
    }

    private static void placeLightAt(ServerWorld world, BlockPos pos) {
        if (!canPlaceLightAt(world, pos)) {
            return;
        }

        int lightLevel = Math.max(
                1,
                Math.min(
                        15,
                        DarknessConfigs.get().server.wardingLamp.lightLevel
                )
        );

        world.setBlockState(
                pos,
                Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, lightLevel),
                Block.NOTIFY_ALL_AND_REDRAW
        );
    }

    private static void removeLightAt(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos).isOf(Blocks.LIGHT)) {
            world.setBlockState(
                    pos,
                    Blocks.AIR.getDefaultState(),
                    Block.NOTIFY_ALL_AND_REDRAW
            );
        }
    }

    public static void cleanup(UUID uuid, MinecraftServer server) {
        LAST_USAGE_TICK.remove(uuid);

        LastLight lastLight = LAST_LIGHT.remove(uuid);

        if (lastLight == null) {
            return;
        }

        ServerWorld world = server.getWorld(lastLight.worldKey());

        if (world != null) {
            removeLightAt(world, lastLight.pos());
        }
    }

    public static void cleanup(UUID uuid, ServerWorld fallbackWorld) {
        LAST_USAGE_TICK.remove(uuid);

        LastLight lastLight = LAST_LIGHT.remove(uuid);

        if (lastLight == null) {
            return;
        }

        if (fallbackWorld.getRegistryKey().equals(lastLight.worldKey())) {
            removeLightAt(fallbackWorld, lastLight.pos());
        }
    }

    private static void syncLampStack(ServerPlayerEntity player) {
        player.getInventory().markDirty();
        player.playerScreenHandler.sendContentUpdates();
        player.currentScreenHandler.syncState();
    }

    private static ItemStack getHeldLamp(ServerPlayerEntity player) {
        if (player.getMainHandStack().isOf(ErodedBlocks.WARDING_LANTERN.asItem())) {
            return player.getMainHandStack();
        }

        if (player.getOffHandStack().isOf(ErodedBlocks.WARDING_LANTERN.asItem())) {
            return player.getOffHandStack();
        }

        return ItemStack.EMPTY;
    }
}
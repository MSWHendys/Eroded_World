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

        ensureLampMaxDamage(lamp, lampCfg.durationSeconds);

        BlockPos playerPos = player.getBlockPos();
        int skyLight = world.getLightLevel(LightType.SKY, playerPos);

        boolean isUnderground = playerPos.getY() < lampCfg.undergroundY || !world.isSkyVisible(playerPos);
        boolean isDarkEnough = skyLight <= lampCfg.skyLightMax && isUnderground;

        if (!isDarkEnough) {
            cleanup(uuid, server);
            return;
        }

        if (server.getTicks() % 20 == 0) {
            int newDamage = lamp.getDamage() + 1;
            lamp.setDamage(newDamage);

            if (newDamage >= lamp.getMaxDamage()) {
                lamp.setCount(0);
                cleanup(uuid, server);
                return;
            }
        }

        BlockPos wantedPos = findLightPos(world, playerPos);
        if (wantedPos == null) {
            cleanup(uuid, server);
            return;
        }

        LastLight oldLight = LAST_LIGHT.get(uuid);

        if (oldLight != null) {
            if (oldLight.worldKey().equals(world.getRegistryKey())) {
                BlockPos oldPos = oldLight.pos();

                if (player.squaredDistanceTo(oldPos.getX() + 0.5, oldPos.getY() + 0.5, oldPos.getZ() + 0.5) < 2.25) {
                    if (world.getBlockState(oldPos).isOf(Blocks.LIGHT)) return;
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

    private static void ensureLampMaxDamage(ItemStack lamp, int durationSeconds) {
        int targetMax = Math.max(2, durationSeconds);

        if (lamp.getMaxDamage() != targetMax) {
            lamp.set(DataComponentTypes.MAX_DAMAGE, targetMax);

            if (lamp.getDamage() >= targetMax) {
                lamp.setDamage(targetMax - 1);
            }
        }
    }

    private static BlockPos findLightPos(ServerWorld world, BlockPos playerPos) {
        if (canPlaceLightAt(world, playerPos.up())) return playerPos.up();
        if (canPlaceLightAt(world, playerPos)) return playerPos;
        return null;
    }

    private static boolean canPlaceLightAt(ServerWorld world, BlockPos pos) {
        return world.isAir(pos) || world.getBlockState(pos).isOf(Blocks.LIGHT);
    }

    private static void placeLightAt(ServerWorld world, BlockPos pos) {
        if (!canPlaceLightAt(world, pos)) {
            return;
        }

        int lightLevel = Math.min(15, Math.max(1, DarknessConfigs.get().server.wardingLamp.lightLevel));

        world.setBlockState(
                pos,
                Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, lightLevel),
                Block.NOTIFY_ALL
        );
    }

    private static void removeLightAt(ServerWorld world, BlockPos pos) {
        if (world != null && world.getBlockState(pos).isOf(Blocks.LIGHT)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    public static void cleanup(UUID uuid, MinecraftServer server) {
        LastLight lastLight = LAST_LIGHT.remove(uuid);
        if (lastLight != null) {
            ServerWorld world = server.getWorld(lastLight.worldKey());
            if (world != null) removeLightAt(world, lastLight.pos());
        }
    }

    public static void cleanup(UUID uuid, ServerWorld fallbackWorld) {
        LastLight lastLight = LAST_LIGHT.remove(uuid);
        if (lastLight != null && fallbackWorld.getRegistryKey().equals(lastLight.worldKey())) {
            removeLightAt(fallbackWorld, lastLight.pos());
        }
    }

    private static ItemStack getHeldLamp(ServerPlayerEntity player) {
        if (player.getMainHandStack().isOf(ErodedBlocks.WARDING_LANTERN.asItem())) return player.getMainHandStack();
        if (player.getOffHandStack().isOf(ErodedBlocks.WARDING_LANTERN.asItem())) return player.getOffHandStack();
        return ItemStack.EMPTY;
    }
}
package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ErodedLampHandler {

    private static final Map<UUID, LastLight> LAST_LIGHT = new ConcurrentHashMap<>();

    private ErodedLampHandler() {
    }

    private record LastLight(ResourceKey<Level> worldKey, BlockPos pos) {
    }

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

        if (lamp.isEmpty()) {
            cleanup(uuid, server);
            return;
        }

        var lampCfg = DarknessConfigs.get().server.wardingLamp;

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

        BlockPos wantedPos = findLightPos(world, playerPos);
        if (wantedPos == null) {
            cleanup(uuid, server);
            return;
        }

        LastLight oldLight = LAST_LIGHT.get(uuid);

        if (oldLight != null) {
            if (oldLight.worldKey().equals(world.dimension())) {
                BlockPos oldPos = oldLight.pos();

                if (player.distanceToSqr(oldPos.getX() + 0.5, oldPos.getY() + 0.5, oldPos.getZ() + 0.5) < 2.25) {
                    if (world.getBlockState(oldPos).is(Blocks.LIGHT)) return;
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
            LAST_LIGHT.put(uuid, new LastLight(world.dimension(), wantedPos));
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

    private static BlockPos findLightPos(ServerLevel world, BlockPos playerPos) {
        if (canPlaceLightAt(world, playerPos.above())) return playerPos.above();
        if (canPlaceLightAt(world, playerPos)) return playerPos;
        return null;
    }

    private static boolean canPlaceLightAt(ServerLevel world, BlockPos pos) {
        return world.isEmptyBlock(pos) || world.getBlockState(pos).is(Blocks.LIGHT);
    }

    private static void placeLightAt(ServerLevel world, BlockPos pos) {
        if (!canPlaceLightAt(world, pos)) {
            return;
        }

        int lightLevel = Math.min(15, Math.max(1, DarknessConfigs.get().server.wardingLamp.lightLevel));

        world.setBlock(
                pos,
                Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel),
                Block.UPDATE_ALL
        );
    }

    private static void removeLightAt(ServerLevel world, BlockPos pos) {
        if (world != null && world.getBlockState(pos).is(Blocks.LIGHT)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void cleanup(UUID uuid, MinecraftServer server) {
        LastLight lastLight = LAST_LIGHT.remove(uuid);
        if (lastLight != null) {
            ServerLevel world = server.getLevel(lastLight.worldKey());
            if (world != null) removeLightAt(world, lastLight.pos());
        }
    }

    public static void cleanup(UUID uuid, ServerLevel fallbackWorld) {
        LastLight lastLight = LAST_LIGHT.remove(uuid);
        if (lastLight != null && fallbackWorld.dimension().equals(lastLight.worldKey())) {
            removeLightAt(fallbackWorld, lastLight.pos());
        }
    }

    private static ItemStack getHeldLamp(ServerPlayer player) {
        if (player.getMainHandItem().is(ErodedBlocks.WARDING_LANTERN.asItem())) return player.getMainHandItem();
        if (player.getOffhandItem().is(ErodedBlocks.WARDING_LANTERN.asItem())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }
}

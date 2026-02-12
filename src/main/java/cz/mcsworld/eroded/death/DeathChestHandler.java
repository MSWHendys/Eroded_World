package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.core.ErodedItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DeathChestHandler {

    private DeathChestHandler() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register(DeathChestHandler::onDeath);
    }

    private static boolean onDeath(LivingEntity entity, DamageSource source, float damage) {
        if (!(entity instanceof ServerPlayerEntity player)) return true;
        if (!(player.getWorld() instanceof ServerWorld world)) return true;

        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && s.isOf(ErodedItems.DEATH_COMPASS)) {
                inv.setStack(i, ItemStack.EMPTY);
            }
        }

        boolean hasItems = false;
        for (int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) return true;

        UUID hologramId = UUID.randomUUID();

        BlockPos chestPos = findSurfacePos(world, player.getBlockPos());
        world.setBlockState(chestPos, ErodedBlocks.DEATH_ENDER_CHEST.getDefaultState());

        List<ItemStack> snapshot = new ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            snapshot.add(inv.getStack(i));
            inv.setStack(i, ItemStack.EMPTY);
        }
        player.getInventory().setSelectedSlot(0);

        long deathValue = DeathValueCalculator.calculate(snapshot);

        long baseTimeMs = DeathConfig.get().chest.protectionTicks * 50L;
        double dist = DeathDistanceHelper.getDeathToWorldSpawnDistance(player, chestPos, world.getRegistryKey());
        long extraTimeMs = (dist >= 0) ? Math.round(dist * 450L) : 0L;
        long untilEpochMs = System.currentTimeMillis() + baseTimeMs + extraTimeMs;

        ErodedDeathMemory memory = new ErodedDeathMemory(
                chestPos,
                world.getRegistryKey(),
                untilEpochMs,
                deathValue,
                hologramId
        );

        boolean accepted = ErodedDeathStorage.putIfMoreValuable(player.getUuid(), memory);

        Map<Integer, DeathChestState.StoredStack> stored = DeathChestState.fromInventory(snapshot);
        DeathChestState.get(world).put(chestPos, player.getUuid(), untilEpochMs, stored, hologramId);

        ErodedCompassHandler.onPlayerDeath(player, chestPos, accepted);

        int remainingSeconds = (int) ((untilEpochMs - System.currentTimeMillis()) / 1000L);
        DeathHologramHandler.spawn(
                world,
                chestPos,
                player.getGameProfile(),
                remainingSeconds,
                hologramId
        );

        return true;
    }

    private static BlockPos findSurfacePos(ServerWorld world, BlockPos startPos) {
        if (world.getFluidState(startPos).isEmpty()) {
            return world.getBlockState(startPos).isAir() ? startPos : startPos.up();
        }
        for (int r = 1; r <= 6; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {

                    for (int yOff : new int[]{0, 1, -1}) {
                        BlockPos checkPos = startPos.add(x, yOff, z);

                        if (world.getFluidState(checkPos).isEmpty() && world.getBlockState(checkPos).isReplaceable()) {
                            return checkPos;
                        }
                    }
                }
            }
        }

        BlockPos.Mutable mutablePos = startPos.mutableCopy();
        int searchLimit = Math.min(startPos.getY() + 45, world.getTopYInclusive() - 3);

        while (mutablePos.getY() < searchLimit) {
            if (!world.getFluidState(mutablePos).isEmpty()) {
                mutablePos.move(0, 1, 0);
            } else {
                break;
            }
        }

        return mutablePos.toImmutable();
    }
}
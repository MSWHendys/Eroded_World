package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.core.ErodedItems;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

public final class ErodedCompassHandler {

    private ErodedCompassHandler() {}

    /**
     * Rehydrates the runtime death-memory cache from persistent death-chest
     * SavedData whenever a player joins. ErodedDeathStorage is intentionally
     * runtime-only, so without this step a server restart would make the first
     * compass tick delete an otherwise valid persisted death compass.
     */
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            refreshFromPersistentState(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ErodedDeathStorage.clear(handler.getPlayer().getUUID())
        );
    }

    public static void onPlayerDeath(ServerPlayer player, BlockPos deathPos, boolean accepted) {
        if (!accepted) return;
        giveCompass(player);
    }

    public static void tick(ServerPlayer player) {
        ErodedDeathMemory mem = ErodedDeathStorage.get(player.getUUID());

        // Defensive restart recovery: JOIN normally restores the cache before
        // the first tick, but if a valid compass is already present and the
        // runtime cache is unexpectedly empty, never delete it before checking
        // persistent death-chest state.
        if (mem == null && hasCompass(player)) {
            mem = recoverMemory(player);
        }

        if (mem == null) {
            removeCompass(player);
            return;
        }

        if (mem.isExpired(player.level().getServer().getTickCount()) || mem.isResolved()) {

            removeCompass(player);
            ErodedDeathStorage.clear(player.getUUID());
            return;
        }

        updateCompassTarget(player, mem);
    }

    /**
     * Restores the most valuable still-active death target owned by this player
     * from per-dimension persistent DeathChestState. This preserves the original
     * putIfMoreValuable semantics across a full JVM/server restart.
     */
    private static ErodedDeathMemory recoverMemory(ServerPlayer player) {
        UUID owner = player.getUUID();
        long now = System.currentTimeMillis();
        ErodedDeathMemory best = null;

        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            DeathChestState state = DeathChestState.getIfPresent(level);
            if (state == null) continue;

            for (DeathChestState.Entry entry : state.all()) {
                if (!entry.owner().equals(owner)) continue;
                if (now >= entry.protectUntilEpochMs()) continue;

                long value = DeathValueCalculator.calculate(
                        DeathChestState.toInventory(entry.items())
                );

                ErodedDeathMemory candidate = new ErodedDeathMemory(
                        entry.pos(),
                        level.dimension(),
                        entry.protectUntilEpochMs(),
                        value,
                        entry.hologramId()
                );

                if (best == null || candidate.getValue() > best.getValue()) {
                    best = candidate;
                }
            }
        }

        if (best != null) {
            ErodedDeathStorage.put(owner, best);
        }

        return best;
    }

    public static void refreshFromPersistentState(ServerPlayer player) {
        // The persistent chest state is authoritative. Clear any stale runtime
        // target first so this method is also safe after resolving one of
        // several death chests.
        ErodedDeathStorage.clear(player.getUUID());
        ErodedDeathMemory mem = recoverMemory(player);

        if (mem == null) {
            // No active persistent remains exist. Remove only stale Eroded
            // compasses that may have been saved in player.dat.
            removeCompass(player);
            ErodedCompassSyncHandler.forceSync(player);
            return;
        }

        giveCompass(player);
        updateCompassTarget(player, mem);
        ErodedCompassSyncHandler.forceSync(player);
    }

    private static boolean hasCompass(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ErodedItems.DEATH_COMPASS)) {
                return true;
            }
        }
        return false;
    }

    private static void giveCompass(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ErodedItems.DEATH_COMPASS)) return;
        }

        ItemStack compass = new ItemStack(ErodedItems.DEATH_COMPASS);
        inv.add(compass);
        player.inventoryMenu.broadcastChanges();
    }

    private static void updateCompassTarget(ServerPlayer player, ErodedDeathMemory mem) {
        Inventory inv = player.getInventory();
        ServerLevel currentWorld = player.level();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ErodedItems.DEATH_COMPASS)) {

                BlockPos targetPos;
                ResourceKey<Level> targetDimKey;

                if (currentWorld.dimension().equals(mem.getDeathDimension())) {
                    targetPos = mem.getDeathPos();
                    targetDimKey = currentWorld.dimension();
                } else if (currentWorld.dimension().equals(Level.OVERWORLD)) {
                    BlockPos portal = ErodedPortalMemoryState.get(player.level().getServer().getLevel(Level.OVERWORLD))
                            .getOverworldPortal(player.getUUID());
                    targetPos = (portal != null) ? portal : mem.getDeathPos();
                    targetDimKey = Level.OVERWORLD;
                } else {
                    targetPos = mem.getDeathPos();
                    targetDimKey = mem.getDeathDimension();
                }

                GlobalPos newGlobalPos = new GlobalPos(targetDimKey, targetPos);
                LodestoneTracker currentLodestone = stack.get(DataComponents.LODESTONE_TRACKER);

                if (currentLodestone == null || currentLodestone.target().isEmpty() || !currentLodestone.target().get().equals(newGlobalPos)) {
                    stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(newGlobalPos), true));

                    CompoundTag nbt = new CompoundTag();
                    nbt.putLong("ChestPos", mem.getDeathPos().asLong());
                    nbt.putString("DeathDim", mem.getDeathDimension().location().toString());

                    BlockPos portal = ErodedPortalMemoryState.get(player.level().getServer().getLevel(Level.OVERWORLD))
                            .getOverworldPortal(player.getUUID());
                    if (portal != null) nbt.putLong("PortalPos", portal.asLong());

                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                    player.inventoryMenu.broadcastChanges();
                }
                break;
            }
        }
    }

    private static void removeCompass(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ErodedItems.DEATH_COMPASS)) {
                inv.removeItemNoUpdate(i);
                player.inventoryMenu.broadcastChanges();
            }
        }
    }
    public static void forceRemove(ServerPlayer player) {
        removeCompass(player);
        ErodedDeathStorage.clear(player.getUUID());
    }

}
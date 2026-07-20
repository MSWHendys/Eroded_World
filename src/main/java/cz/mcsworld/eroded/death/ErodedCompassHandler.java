package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.core.ErodedItems;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class ErodedCompassHandler {

    private ErodedCompassHandler() {}

    public static void onPlayerDeath(ServerPlayer player, BlockPos deathPos, boolean accepted) {
        if (!accepted) return;
        giveCompass(player);
    }

    public static void tick(ServerPlayer player) {
        ErodedDeathMemory mem = ErodedDeathStorage.get(player.getUUID());

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
                ResourceKey<@NotNull Level> targetDimKey;

                if (currentWorld.dimension().equals(mem.getDeathDimension())) {
                    targetPos = mem.getDeathPos();
                    targetDimKey = currentWorld.dimension();
                } else if (currentWorld.dimension().equals(Level.OVERWORLD)) {
                    BlockPos portal = ErodedPortalMemoryState.get(Objects.requireNonNull(player.level().getServer().getLevel(Level.OVERWORLD)))
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
                    nbt.putString("DeathDim", mem.getDeathDimension().identifier().toString());

                    BlockPos portal = ErodedPortalMemoryState.get(Objects.requireNonNull(player.level().getServer().getLevel(Level.OVERWORLD)))
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
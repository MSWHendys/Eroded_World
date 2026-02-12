package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.core.ErodedItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

import java.util.Optional;

public final class ErodedCompassHandler {

    private ErodedCompassHandler() {}

    public static void onPlayerDeath(ServerPlayerEntity player, BlockPos deathPos, boolean accepted) {
        if (!accepted) return;
        giveCompass(player);
    }

    public static void tick(ServerPlayerEntity player) {
        ErodedDeathMemory mem = ErodedDeathStorage.get(player.getUuid());

        if (mem == null) {
            removeCompass(player);
            return;
        }

        if (mem.isExpired(player.getServer().getTicks()) || mem.isResolved()) {

            removeCompass(player);
            ErodedDeathStorage.clear(player.getUuid());
            return;
        }

        updateCompassTarget(player, mem);
    }

    private static void giveCompass(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(ErodedItems.DEATH_COMPASS)) return;
        }

        ItemStack compass = new ItemStack(ErodedItems.DEATH_COMPASS);
        inv.insertStack(compass);
        player.playerScreenHandler.sendContentUpdates();
    }

    private static void updateCompassTarget(ServerPlayerEntity player, ErodedDeathMemory mem) {
        PlayerInventory inv = player.getInventory();
        ServerWorld currentWorld = player.getWorld();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(ErodedItems.DEATH_COMPASS)) {

                BlockPos targetPos;
                RegistryKey<World> targetDimKey;

                if (currentWorld.getRegistryKey().equals(mem.getDeathDimension())) {
                    targetPos = mem.getDeathPos();
                    targetDimKey = currentWorld.getRegistryKey();
                } else if (currentWorld.getRegistryKey().equals(World.OVERWORLD)) {
                    BlockPos portal = ErodedPortalMemoryState.get(player.getServer().getWorld(World.OVERWORLD))
                            .getOverworldPortal(player.getUuid());
                    targetPos = (portal != null) ? portal : mem.getDeathPos();
                    targetDimKey = World.OVERWORLD;
                } else {
                    targetPos = mem.getDeathPos();
                    targetDimKey = mem.getDeathDimension();
                }

                GlobalPos newGlobalPos = new GlobalPos(targetDimKey, targetPos);
                LodestoneTrackerComponent currentLodestone = stack.get(DataComponentTypes.LODESTONE_TRACKER);

                if (currentLodestone == null || currentLodestone.target().isEmpty() || !currentLodestone.target().get().equals(newGlobalPos)) {
                    stack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.of(newGlobalPos), true));

                    NbtCompound nbt = new NbtCompound();
                    nbt.putLong("ChestPos", mem.getDeathPos().asLong());
                    nbt.putString("DeathDim", mem.getDeathDimension().getValue().toString());

                    BlockPos portal = ErodedPortalMemoryState.get(player.getServer().getWorld(World.OVERWORLD))
                            .getOverworldPortal(player.getUuid());
                    if (portal != null) nbt.putLong("PortalPos", portal.asLong());

                    stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                    player.playerScreenHandler.sendContentUpdates();
                }
                break;
            }
        }
    }

    private static void removeCompass(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(ErodedItems.DEATH_COMPASS)) {
                inv.removeStack(i);
                player.playerScreenHandler.sendContentUpdates();
            }
        }
    }
    public static void forceRemove(ServerPlayerEntity player) {
        removeCompass(player);
        ErodedDeathStorage.clear(player.getUuid());
    }

}
package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and persists a player's death chest.
 *
 * <p>The actual hook lives in {@code PlayerDeathInventoryMixin}. It runs from
 * vanilla's {@code Player.dropEquipment(ServerLevel)}, which is only reached
 * for a committed death. This deliberately avoids trying to predict death from
 * pre-mitigation damage events.</p>
 */
public final class DeathChestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ErodedDeath");

    private DeathChestHandler() {}

    /**
     * Kept for the existing mod bootstrap. Death capture itself is installed
     * through the mixin, so no Fabric damage/death callback is required here.
     */
    public static void register() {
        // Intentionally empty.
    }

    /**
     * Moves the inventory into persistent death-chest state.
     *
     * @return {@code true} when vanilla inventory dropping must be cancelled;
     *         {@code false} when Eroded could not safely take ownership and
     *         vanilla should perform its normal death drop.
     */
    public static boolean captureDeathInventory(ServerPlayer player) {
        ServerLevel world = player.level();
        List<ItemStack> snapshot = snapshotInventory(player);

        // If the player only had an old death compass (or nothing), consume the
        // compass and suppress vanilla dropping. No empty remains are created.
        if (snapshot.isEmpty()) {
            clearInventory(player);
            return true;
        }

        BlockPos deathPos = player.blockPosition().immutable();
        BlockPos chestPos = findSurfacePos(world, deathPos);
        DeathChestState state = DeathChestState.get(world);

        // Never overwrite another live death-chest record at the same position.
        chestPos = findFreeChestPos(world, state, chestPos);
        if (chestPos == null) {
            LOGGER.error("Nelze najit bezpecnou pozici pro Death Chest hrace {}. Pouzije se vanilla drop.",
                    player.getGameProfile().name());
            return false;
        }

        UUID hologramId = UUID.randomUUID();
        long deathValue = DeathValueCalculator.calculate(snapshot);
        long baseTimeMs = DeathProtectionCalculator.calculateProtectionMillis(player, chestPos);
        long untilEpochMs = System.currentTimeMillis() + baseTimeMs;
        Map<Integer, DeathChestState.StoredStack> stored = DeathChestState.fromInventory(snapshot);
        BlockState previousState = world.getBlockState(chestPos);

        boolean stateStored = false;
        boolean blockPlaced = false;

        try {
            // Persist the item snapshot first. If normal code fails after this
            // point but before inventory clearing, rollback below keeps vanilla
            // death handling authoritative.
            state.put(
                    chestPos,
                    player.getUUID(),
                    untilEpochMs,
                    stored,
                    hologramId
            );
            stateStored = true;

            blockPlaced = world.setBlock(
                    chestPos,
                    ErodedBlocks.DEATH_ENDER_CHEST.defaultBlockState(),
                    3
            );

            if (!blockPlaced) {
                throw new IllegalStateException("Nepodarilo se umistit Death Chest block na " + chestPos);
            }

            // Ownership transfers only after both persistent state and the block
            // exist. From here vanilla must not drop these stacks again.
            clearInventory(player);

        } catch (Exception e) {
            if (stateStored) {
                state.remove(chestPos);
            }

            if (blockPlaced) {
                try {
                    world.setBlock(chestPos, previousState, 3);
                } catch (Exception rollbackError) {
                    LOGGER.error("Selhal rollback Death Chest blocku na {}", chestPos, rollbackError);
                }
            }

            LOGGER.error(
                    "Chyba pri transakcnim vytvareni Death Chest hrace {}. Inventar zustava vanilla systemu.",
                    player.getGameProfile().name(),
                    e
            );
            return false;
        }

        // Everything below is post-commit metadata/visual state. Failure here
        // must never cause the inventory to be dropped a second time.
        try {
            ErodedDeathMemory memory = new ErodedDeathMemory(
                    chestPos,
                    world.dimension(),
                    untilEpochMs,
                    deathValue,
                    hologramId
            );
            ErodedDeathStorage.putIfMoreValuable(player.getUUID(), memory);
        } catch (Exception e) {
            LOGGER.error("Death Chest je ulozen, ale nepodarilo se ulozit death-memory hrace {}.",
                    player.getGameProfile().name(), e);
        }

        try {
            DeathHologramHandler.spawn(
                    world,
                    chestPos,
                    player.getGameProfile(),
                    (int) (baseTimeMs / 1000L),
                    hologramId
            );
        } catch (Exception e) {
            LOGGER.error("Death Chest je ulozen, ale nepodarilo se vytvorit hologram na {}.", chestPos, e);
        }

        return true;
    }

    private static List<ItemStack> snapshotInventory(ServerPlayer player) {
        List<ItemStack> snapshot = new ArrayList<>();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.is(ErodedItems.DEATH_COMPASS)) {
                continue;
            }
            snapshot.add(stack.copy());
        }

        return snapshot;
    }

    private static void clearInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            player.getInventory().setItem(i, ItemStack.EMPTY);
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    private static BlockPos findSurfacePos(ServerLevel world, BlockPos startPos) {
        BlockPos.MutableBlockPos mutable = startPos.mutable();
        while (world.getBlockState(mutable).isAir() && mutable.getY() > world.getMinY()) {
            mutable.move(0, -1, 0);
        }
        return (mutable.getY() < startPos.getY()) ? mutable.above().immutable() : startPos;
    }

    /**
     * Avoids silently replacing another Eroded death chest when two deaths use
     * the same coordinates. Prefer the original position, then nearby air.
     */
    private static BlockPos findFreeChestPos(
            ServerLevel world,
            DeathChestState state,
            BlockPos preferred
    ) {
        if (state.get(preferred) == null) {
            return preferred;
        }

        for (int radius = 1; radius <= 4; radius++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                            continue;
                        }

                        BlockPos candidate = preferred.offset(dx, dy, dz);
                        if (state.get(candidate) == null && world.getBlockState(candidate).isAir()) {
                            return candidate.immutable();
                        }
                    }
                }
            }
        }

        return null;
    }
}

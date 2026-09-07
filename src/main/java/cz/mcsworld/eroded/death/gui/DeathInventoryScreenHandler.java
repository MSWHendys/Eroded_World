package cz.mcsworld.eroded.death.gui;

import cz.mcsworld.eroded.death.DeathChestState;
import cz.mcsworld.eroded.death.DeathHologramHandler;
import cz.mcsworld.eroded.death.ErodedCompassHandler;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DeathInventoryScreenHandler extends AbstractContainerMenu {

    private static final int SIZE = DeathChestState.SIZE;
    private static final double MAX_USE_DISTANCE_SQR = 64.0D;

    private final ServerLevel world;
    private final BlockPos pos;
    private final UUID playerId;
    private final UUID sessionToken;
    private final BackedContainer inventory;
    private final DeathChestState state;
    private boolean closing;

    public DeathInventoryScreenHandler(
            int syncId,
            Inventory playerInv,
            ServerLevel world,
            BlockPos pos,
            UUID sessionToken
    ) {
        super(MenuType.GENERIC_9x6, syncId);

        this.world = world;
        this.pos = pos.immutable();
        this.playerId = playerInv.player.getUUID();
        this.sessionToken = sessionToken;
        this.state = DeathChestState.get(world);
        this.inventory = new BackedContainer();

        DeathChestState.Entry entry = state.get(pos);
        if (entry != null && state.isOpenBy(pos, playerId, sessionToken)) {
            inventory.load(DeathChestState.toInventory(entry.items()));
        }

        inventory.startOpen(playerInv.player);

        for (int i = 0; i < SIZE; i++) {
            int x = i % 9;
            int y = i / 9;
            addSlot(new Slot(inventory, i, 8 + x * 18, 18 + y * 18));
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(
                        playerInv,
                        x + y * 9 + 9,
                        8 + x * 18,
                        140 + y * 18
                ));
            }
        }

        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(playerInv, x, 8 + x * 18, 198));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (world.isClientSide()) return;

        // If this is a stale menu, it must never materialize its copied items.
        if (!state.isOpenBy(pos, playerId, sessionToken)) {
            inventory.discardWithoutSync();
            return;
        }

        closing = true;
        DeathChestState.Entry entry = state.get(pos);

        try {
            // The persistent entry is authoritative. If it disappeared while
            // this GUI was open, dropping the local copy could duplicate items.
            if (entry == null) {
                inventory.discardWithoutSync();
                return;
            }

            for (ItemStack stack : inventory.getItems()) {
                if (stack.isEmpty()) continue;

                world.addFreshEntity(new ItemEntity(
                        world,
                        pos.getX() + 0.5,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5,
                        stack.copy()
                ));
            }

            inventory.discardWithoutSync();
            state.remove(pos);

            if (world.getBlockState(pos).is(ErodedBlocks.DEATH_ENDER_CHEST)) {
                world.destroyBlock(pos, false);
            }

            DeathHologramHandler.removeAt(world, pos, entry.hologramId());

            if (player instanceof ServerPlayer sp) {
                ErodedCompassHandler.refreshFromPersistentState(sp);
            }
        } finally {
            // state.remove() already clears the lock, but releaseOpen() is also
            // safe when the entry vanished through another recovery path.
            state.releaseOpen(pos, sessionToken);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (world.isClientSide()) {
            return true;
        }

        if (!player.getUUID().equals(playerId)) {
            return false;
        }

        if (!state.isOpenBy(pos, playerId, sessionToken)) {
            return false;
        }

        if (state.get(pos) == null) {
            return false;
        }

        if (!world.getBlockState(pos).is(ErodedBlocks.DEATH_ENDER_CHEST)) {
            return false;
        }

        double dx = player.getX() - (pos.getX() + 0.5D);
        double dy = player.getY() - (pos.getY() + 0.5D);
        double dz = player.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= MAX_USE_DISTANCE_SQR;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    private final class BackedContainer extends SimpleContainer {
        private boolean loading;

        private BackedContainer() {
            super(SIZE);
        }

        private void load(List<ItemStack> items) {
            loading = true;
            try {
                for (int i = 0; i < SIZE; i++) {
                    super.setItem(i, items.get(i));
                }
            } finally {
                loading = false;
            }
        }

        private void discardWithoutSync() {
            loading = true;
            try {
                super.clearContent();
            } finally {
                loading = false;
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();

            if (loading || closing) {
                return;
            }

            state.updateItems(
                    pos,
                    playerId,
                    sessionToken,
                    getItems()
            );
        }
    }
}

package cz.mcsworld.eroded.death.gui;

import cz.mcsworld.eroded.death.DeathChestState;
import cz.mcsworld.eroded.death.DeathHologramHandler;
import cz.mcsworld.eroded.death.ErodedCompassHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class DeathInventoryScreenHandler extends AbstractContainerMenu {

    private static final int SIZE = DeathChestState.SIZE;

    private final ServerLevel world;
    private final BlockPos pos;
    private final SimpleContainer inventory;
    private final DeathChestState state;

    public DeathInventoryScreenHandler(
            int syncId,
            Inventory playerInv,
            ServerLevel world,
            BlockPos pos
    ) {
        super(MenuType.GENERIC_9x6, syncId);

        this.world = world;
        this.pos = pos;
        this.state = DeathChestState.get(world);
        this.inventory = new SimpleContainer(SIZE);

        DeathChestState.Entry entry = state.get(pos);
        if (entry != null) {
            List<ItemStack> items =
                    DeathChestState.toInventory(entry.items());
            for (int i = 0; i < SIZE; i++) {
                inventory.setItem(i, items.get(i));
            }
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
    public void removed(@NotNull Player player) {
        super.removed(player);

        if (world.isClientSide()) return;

        DeathChestState.Entry entry = state.get(pos);

        for (ItemStack stack : inventory.getItems()) {
            if (!stack.isEmpty()) {
                world.addFreshEntity(
                        new net.minecraft.world.entity.item.ItemEntity(
                                world,
                                pos.getX() + 0.5,
                                pos.getY() + 1.0,
                                pos.getZ() + 0.5,
                                stack.copy()
                        )
                );
            }
        }

        inventory.clearContent();

        state.remove(pos);

        boolean hadBlock = !world.getBlockState(pos).isAir();
        world.destroyBlock(pos, false);


        if (entry != null) {
            DeathHologramHandler.removeById(world, entry.hologramId());

        }

        if (player instanceof ServerPlayer sp) {
            ErodedCompassHandler.forceRemove(sp);

        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slot) {
        return ItemStack.EMPTY;
    }
}

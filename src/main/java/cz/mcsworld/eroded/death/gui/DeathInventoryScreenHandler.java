package cz.mcsworld.eroded.death.gui;

import cz.mcsworld.eroded.death.DeathChestState;
import cz.mcsworld.eroded.death.DeathHologramHandler;
import cz.mcsworld.eroded.death.ErodedCompassHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class DeathInventoryScreenHandler extends ScreenHandler {

    private static final int SIZE = DeathChestState.SIZE;

    private final ServerWorld world;
    private final BlockPos pos;
    private final SimpleInventory inventory;
    private final DeathChestState state;

    public DeathInventoryScreenHandler(
            int syncId,
            PlayerInventory playerInv,
            ServerWorld world,
            BlockPos pos
    ) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);

        this.world = world;
        this.pos = pos;
        this.state = DeathChestState.get(world);
        this.inventory = new SimpleInventory(SIZE);

        DeathChestState.Entry entry = state.get(pos);
        if (entry != null) {
            List<ItemStack> items =
                    DeathChestState.toInventory(entry.items());
            for (int i = 0; i < SIZE; i++) {
                inventory.setStack(i, items.get(i));
            }
        }

        inventory.onOpen(playerInv.player);

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
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);

        if (world.isClient) return;

        DeathChestState.Entry entry = state.get(pos);

        for (ItemStack stack : inventory.getHeldStacks()) {
            if (!stack.isEmpty()) {
                world.spawnEntity(
                        new net.minecraft.entity.ItemEntity(
                                world,
                                pos.getX() + 0.5,
                                pos.getY() + 1.0,
                                pos.getZ() + 0.5,
                                stack.copy()
                        )
                );
            }
        }

        inventory.clear();

        state.remove(pos);

        boolean hadBlock = !world.getBlockState(pos).isAir();
        world.breakBlock(pos, false);


        if (entry != null) {
            DeathHologramHandler.removeById(world, entry.hologramId());

        }

        if (player instanceof ServerPlayerEntity sp) {
            ErodedCompassHandler.forceRemove(sp);

        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}

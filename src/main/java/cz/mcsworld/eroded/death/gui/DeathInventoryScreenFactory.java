package cz.mcsworld.eroded.death.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public class DeathInventoryScreenFactory
        implements MenuProvider {

    private final ServerLevel world;
    private final BlockPos pos;

    public DeathInventoryScreenFactory(ServerLevel world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("eroded.death.chest.title");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int syncId,
            @NotNull Inventory inv,
            net.minecraft.world.entity.player.@NotNull Player player
    ) {
        return new DeathInventoryScreenHandler(
                syncId,
                inv,
                world,
                pos
        );
    }
}

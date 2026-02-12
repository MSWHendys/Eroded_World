package cz.mcsworld.eroded.death.gui;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class DeathInventoryScreenFactory
        implements NamedScreenHandlerFactory {

    private final ServerWorld world;
    private final BlockPos pos;

    public DeathInventoryScreenFactory(ServerWorld world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("eroded.death.chest.title");
    }

    @Override
    public ScreenHandler createMenu(
            int syncId,
            PlayerInventory inv,
            net.minecraft.entity.player.PlayerEntity player
    ) {
        return new DeathInventoryScreenHandler(
                syncId,
                inv,
                world,
                pos
        );
    }
}

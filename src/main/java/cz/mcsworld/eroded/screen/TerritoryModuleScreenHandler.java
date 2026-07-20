package cz.mcsworld.eroded.screen;

import cz.mcsworld.eroded.core.ErodedScreenHandlers;
import cz.mcsworld.eroded.protection.TerritoryClaim;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TerritoryModuleScreenHandler extends AbstractContainerMenu {

    private final BlockPos anchorPos;

    public TerritoryModuleScreenHandler(
            int syncId,
            Inventory playerInventory,
            TerritoryModuleScreenData data
    ) {
        super(ErodedScreenHandlers.TERRITORY_MODULE, syncId);
        this.anchorPos = data.anchorPos();
    }

    public BlockPos getAnchorPos() {
        return anchorPos;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        if (!(player.level() instanceof ServerLevel serverWorld)) {
            return false;
        }

        if (player.distanceToSqr(
                anchorPos.getX() + 0.5,
                anchorPos.getY() + 0.5,
                anchorPos.getZ() + 0.5
        ) > 64.0) {
            return false;
        }

        TerritoryClaim claim = TerritoryProtectionManager.getAnchorClaim(serverWorld, anchorPos);

        return claim != null && TerritoryProtectionManager.canManageClaim(serverPlayer, claim);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
package cz.mcsworld.eroded.screen;

import cz.mcsworld.eroded.core.ErodedScreenHandlers;
import cz.mcsworld.eroded.protection.TerritoryClaim;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class TerritoryModuleScreenHandler extends ScreenHandler {

    private final BlockPos anchorPos;

    public TerritoryModuleScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            TerritoryModuleScreenData data
    ) {
        super(ErodedScreenHandlers.TERRITORY_MODULE, syncId);
        this.anchorPos = data.anchorPos();
    }

    public BlockPos getAnchorPos() {
        return anchorPos;
    }

    @Override
    public boolean canUse(PlayerEntity player) {

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }

        if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
            return false;
        }

        if (player.squaredDistanceTo(
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
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
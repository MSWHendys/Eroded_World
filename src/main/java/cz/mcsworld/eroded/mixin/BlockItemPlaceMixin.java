package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.world.territory.TerritoryTracker;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemPlaceMixin {

    @Inject(
            method = "place",
            at = @At("RETURN")
    )
    private void eroded$afterBlockPlaced(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {

        ActionResult result = cir.getReturnValue();
        if (result == null || !result.isAccepted()) return;

        if (!(context.getWorld() instanceof ServerWorld world)) return;

        BlockPos placedPos = context.getBlockPos();
        BlockState placedState = world.getBlockState(placedPos);

        TerritoryTracker.onBlockPlaced(world, placedPos, placedState);
    }
}

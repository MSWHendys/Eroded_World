package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.world.territory.TerritoryTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
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
    private void eroded$afterBlockPlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {

        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) return;

        if (!(context.getLevel() instanceof ServerLevel world)) return;

        BlockPos placedPos = context.getClickedPos();
        BlockState placedState = world.getBlockState(placedPos);

        TerritoryTracker.onBlockPlaced(world, placedPos, placedState);
    }
}

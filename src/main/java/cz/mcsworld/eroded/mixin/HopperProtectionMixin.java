package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.InventoryTransferProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(HopperBlockEntity.class)
public abstract class HopperProtectionMixin {

    @Inject(
            method = "tryMoveItems",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void eroded$preventProtectedHopperTransfer(
            Level world,
            BlockPos pos,
            BlockState state,
            HopperBlockEntity blockEntity,
            BooleanSupplier booleanSupplier,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }

        BlockPos inputPos = pos.above();

        Direction outputDirection = state.getValue(HopperBlock.FACING);
        BlockPos outputPos = pos.relative(outputDirection);

        if (!InventoryTransferProtectionManager.canHopperWork(
                serverWorld,
                pos,
                inputPos,
                outputPos
        )) {
            cir.setReturnValue(false);
        }
    }
}
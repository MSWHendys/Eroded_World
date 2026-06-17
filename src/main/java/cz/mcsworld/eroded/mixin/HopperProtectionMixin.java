package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.InventoryTransferProtectionManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(HopperBlockEntity.class)
public abstract class HopperProtectionMixin {

    @Inject(
            method = "insertAndExtract",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void eroded$preventProtectedHopperTransfer(
            World world,
            BlockPos pos,
            BlockState state,
            HopperBlockEntity blockEntity,
            BooleanSupplier booleanSupplier,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        BlockPos inputPos = pos.up();

        Direction outputDirection = state.get(HopperBlock.FACING);
        BlockPos outputPos = pos.offset(outputDirection);

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
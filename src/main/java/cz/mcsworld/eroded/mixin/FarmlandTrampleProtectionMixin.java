package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.MobGriefingProtectionManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandTrampleProtectionMixin {

    @Redirect(
            method = "onLandedUpon",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/FarmlandBlock;setToDirt(Lnet/minecraft/entity/Entity;Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"
            )
    )
    private void eroded$preventFarmlandTurningToDirtInClaim(
            Entity entity,
            BlockState state,
            World world,
            BlockPos pos
    ) {
        if (world instanceof ServerWorld serverWorld
                && !MobGriefingProtectionManager.canMobModifyAt(serverWorld, pos)) {
            return;
        }

        FarmlandBlockInvoker.eroded$setToDirt(entity, state, world, pos);
    }
}
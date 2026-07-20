package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.MobGriefingProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FarmBlock.class)
public abstract class FarmlandTrampleProtectionMixin {

    @Redirect(
            method = "fallOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FarmBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
            )
    )
    private void eroded$preventFarmlandTurningToDirtInClaim(
            Entity entity,
            BlockState state,
            Level world,
            BlockPos pos
    ) {
        if (world instanceof ServerLevel serverWorld
                && !MobGriefingProtectionManager.canMobModifyAt(serverWorld, pos)) {
            return;
        }

        FarmlandBlockInvoker.eroded$setToDirt(entity, state, world, pos);
    }
}
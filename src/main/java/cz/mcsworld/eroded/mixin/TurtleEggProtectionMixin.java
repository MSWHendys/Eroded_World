package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.MobGriefingProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TurtleEggBlock.class)
public abstract class TurtleEggProtectionMixin {

    @Inject(
            method = "destroyEgg",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventTurtleEggBreakingInClaim(
            Level world,
            BlockState state,
            BlockPos pos,
            Entity entity,
            int inverseChance,
            CallbackInfo ci
    ) {
        if (world instanceof ServerLevel serverWorld
                && !MobGriefingProtectionManager.canMobModifyAt(serverWorld, pos)) {
            ci.cancel();
        }
    }
}
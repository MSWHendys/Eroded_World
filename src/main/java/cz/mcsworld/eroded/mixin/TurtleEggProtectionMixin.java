package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.protection.MobGriefingProtectionManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.TurtleEggBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TurtleEggBlock.class)
public abstract class TurtleEggProtectionMixin {

    @Inject(
            method = "tryBreakEgg",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$preventTurtleEggBreakingInClaim(
            World world,
            BlockState state,
            BlockPos pos,
            Entity entity,
            int inverseChance,
            CallbackInfo ci
    ) {
        if (world instanceof ServerWorld serverWorld
                && !MobGriefingProtectionManager.canMobModifyAt(serverWorld, pos)) {
            ci.cancel();
        }
    }
}
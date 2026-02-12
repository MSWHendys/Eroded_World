 package cz.mcsworld.eroded.death;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class DeathChestProtection {

    private DeathChestProtection() {}

    public static boolean isProtected(
            ServerWorld world,
            BlockPos pos
    ) {
        return DeathChestState.get(world).isProtected(pos);
    }
}

 package cz.mcsworld.eroded.death;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class DeathChestProtection {

    private DeathChestProtection() {}

    public static boolean isProtected(
            ServerLevel world,
            BlockPos pos
    ) {
        return DeathChestState.get(world).isProtected(pos);
    }
}

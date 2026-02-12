package cz.mcsworld.eroded.client.compass;

import cz.mcsworld.eroded.client.data.ErodedCompassClientData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public final class ErodedCompassDistanceResolver {

    private ErodedCompassDistanceResolver() {}

    public static double getDistanceToDeath() {

        if (!ErodedCompassClientData.isActive())
            return -1;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return -1;

        BlockPos deathPos =
                ErodedCompassClientData.getTarget();
        if (deathPos == null)
            return -1;

        return client.player.getPos()
                .distanceTo(deathPos.toCenterPos());
    }
}

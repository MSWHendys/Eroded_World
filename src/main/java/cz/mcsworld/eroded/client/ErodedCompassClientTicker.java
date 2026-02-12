package cz.mcsworld.eroded.client;

import cz.mcsworld.eroded.client.data.ErodedCompassClientData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public final class ErodedCompassClientTicker {

    private ErodedCompassClientTicker() {}

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!ErodedCompassClientData.isActive()) return;

        ErodedCompassClientData.tickTime();

        BlockPos target = ErodedCompassClientData.getTarget();
        if (target == null) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();

        double tx = target.getX() + 0.5;
        double tz = target.getZ() + 0.5;

        double dx = tx - px;
        double dz = tz - pz;

        double targetAngle = Math.atan2(dz, dx) / (Math.PI * 2.0);
        double playerYaw = MathHelper.floorMod(mc.player.getYaw() / 360.0, 1.0);

        float angle = MathHelper.floorMod(
                (float) (targetAngle - playerYaw),
                1.0F
        );

        ErodedCompassClientData.setAngle(angle);
    }

}

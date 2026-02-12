package cz.mcsworld.eroded.client.input;

import cz.mcsworld.eroded.network.DodgeRequestPacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class DodgeInputHandler {

    private static final long DOUBLE_TAP_WINDOW_MS = 250;
    private static final long SPRINT_SUPPRESS_MS = 400;

    private static long lastTapTime = 0L;
    private static int lastKey = -1;

    private static long suppressSprintUntil = 0L;

    private static boolean waitingForRespawnReset = false;

    private static boolean wDown, aDown, sDown, dDown;

    private DodgeInputHandler() {}

    public static void register() {

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            reset();
            waitingForRespawnReset = true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(DodgeInputHandler::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {

        if (client.player == null || client.world == null) return;

        if (waitingForRespawnReset && client.player.age == 0) {
            reset();
            waitingForRespawnReset = false;
            return;
        }

        if (System.currentTimeMillis() < suppressSprintUntil && !client.player.isUsingItem()) {
            client.player.setSprinting(false);
        }

        if (client.currentScreen != null) return;

        long now = System.currentTimeMillis();

        wDown = handleKey(client, GLFW.GLFW_KEY_W, wDown, new Vec3d(0, 0, 1), now);
        sDown = handleKey(client, GLFW.GLFW_KEY_S, sDown, new Vec3d(0, 0, -1), now);
        aDown = handleKey(client, GLFW.GLFW_KEY_A, aDown, new Vec3d(1, 0, 0), now);
        dDown = handleKey(client, GLFW.GLFW_KEY_D, dDown, new Vec3d(-1, 0, 0), now);
    }

    private static boolean handleKey(
            MinecraftClient client,
            int glfwKey,
            boolean wasDown,
            Vec3d dir,
            long now
    ) {
        boolean isDown = InputUtil.isKeyPressed(
                client.getWindow().getHandle(),
                glfwKey
        );

        if (isDown && !wasDown) {
            onKeyTap(client, dir, now);
        }

        return isDown;
    }

    private static void onKeyTap(MinecraftClient client, Vec3d dir, long now) {

        int key = dirToKey(dir);

        if (lastKey != key || now - lastTapTime > DOUBLE_TAP_WINDOW_MS) {
            lastKey = key;
            lastTapTime = now;
            return;
        }

        client.player.setSprinting(false);
        suppressSprintUntil = now + SPRINT_SUPPRESS_MS;

        ClientPlayNetworking.send(
                new DodgeRequestPacket(
                        (float) dir.x,
                        (float) dir.z
                )
        );

        reset();
    }

    private static int dirToKey(Vec3d dir) {
        if (dir.z > 0) return GLFW.GLFW_KEY_W;
        if (dir.z < 0) return GLFW.GLFW_KEY_S;
        if (dir.x > 0) return GLFW.GLFW_KEY_A;
        return GLFW.GLFW_KEY_D;
    }

    public static void reset() {
        lastTapTime = 0L;
        lastKey = -1;
    }
}

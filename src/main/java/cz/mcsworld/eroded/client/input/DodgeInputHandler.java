package cz.mcsworld.eroded.client.input;

import cz.mcsworld.eroded.network.DodgeRequestPacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class DodgeInputHandler {

    private static final long DOUBLE_TAP_WINDOW_MS = 250L;
    private static final long SPRINT_SUPPRESS_MS = 400L;

    private static long lastTapTime = 0L;
    private static DirectionKey lastKey = null;

    private static long suppressSprintUntil = 0L;
    private static boolean waitingForRespawnReset = false;

    private static boolean forwardDown;
    private static boolean backwardDown;
    private static boolean leftDown;
    private static boolean rightDown;

    private DodgeInputHandler() {}

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            reset();
            waitingForRespawnReset = true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(
                DodgeInputHandler::onClientTick
        );
    }

    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }

        if (waitingForRespawnReset && client.player.tickCount == 0) {
            reset();
            waitingForRespawnReset = false;
            return;
        }

        long now = System.currentTimeMillis();

        if (now < suppressSprintUntil && !client.player.isUsingItem()) {
            client.player.setSprinting(false);
        }

        if (client.gui.screen() != null) {
            resetKeyStates();
            return;
        }

        forwardDown = handleKey(
                client,
                client.options.keyUp,
                forwardDown,
                DirectionKey.FORWARD,
                new Vec3(0.0, 0.0, 1.0),
                now
        );

        backwardDown = handleKey(
                client,
                client.options.keyDown,
                backwardDown,
                DirectionKey.BACKWARD,
                new Vec3(0.0, 0.0, -1.0),
                now
        );

        leftDown = handleKey(
                client,
                client.options.keyLeft,
                leftDown,
                DirectionKey.LEFT,
                new Vec3(1.0, 0.0, 0.0),
                now
        );

        rightDown = handleKey(
                client,
                client.options.keyRight,
                rightDown,
                DirectionKey.RIGHT,
                new Vec3(-1.0, 0.0, 0.0),
                now
        );
    }

    private static boolean handleKey(
            Minecraft client,
            KeyMapping keyMapping,
            boolean wasDown,
            DirectionKey key,
            Vec3 direction,
            long now
    ) {
        boolean isDown = keyMapping.isDown();

        if (isDown && !wasDown) {
            onKeyTap(client, key, direction, now);
        }

        return isDown;
    }

    private static void onKeyTap(
            Minecraft client,
            DirectionKey key,
            Vec3 direction,
            long now
    ) {
        if (lastKey != key || now - lastTapTime > DOUBLE_TAP_WINDOW_MS) {
            lastKey = key;
            lastTapTime = now;
            return;
        }

        if (client.player == null) {
            reset();
            return;
        }

        client.player.setSprinting(false);
        suppressSprintUntil = now + SPRINT_SUPPRESS_MS;

        ClientPlayNetworking.send(
                new DodgeRequestPacket(
                        (float) direction.x,
                        (float) direction.z
                )
        );

        reset();
    }

    public static void reset() {
        lastTapTime = 0L;
        lastKey = null;
        resetKeyStates();
    }

    private static void resetKeyStates() {
        forwardDown = false;
        backwardDown = false;
        leftDown = false;
        rightDown = false;
    }

    private enum DirectionKey {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT
    }
}
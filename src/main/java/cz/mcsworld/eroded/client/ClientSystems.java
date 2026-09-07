package cz.mcsworld.eroded.client;

import cz.mcsworld.eroded.client.audio.HeartbeatClient;
import cz.mcsworld.eroded.client.compass.ErodedCompassHeartbeat;
import cz.mcsworld.eroded.client.util.DarknessHudGuard;
import net.minecraft.client.Minecraft;

public final class ClientSystems {

    private ClientSystems() {}

    public static void clientTick() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;
        HeartbeatClient.tick();
        // Sprint permission is server-authoritative. Do not mutate the physical
        // sprint key here: forcing keySprint.setDown(false) can latch Ctrl off
        // before the server ever observes a sprinting player, which also prevents
        // SprintEnergyHandler from draining Energy. Low-energy/collapse sprint is
        // rejected server-side by SprintEnergyHandler/EnergyMovementHandler.

        DarknessHudGuard.enforceHudVisibility();
        ErodedCompassHeartbeat.clientTick();
    }
}

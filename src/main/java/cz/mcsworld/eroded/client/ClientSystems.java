package cz.mcsworld.eroded.client;

import cz.mcsworld.eroded.client.audio.HeartbeatClient;
import cz.mcsworld.eroded.client.compass.ErodedCompassHeartbeat;
import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.client.util.DarknessHudGuard;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import net.minecraft.client.Minecraft;

public final class ClientSystems {

    private ClientSystems() {}

    public static void clientTick() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;
        HeartbeatClient.tick();
        EnergyConfig cfg = EnergyConfig.get();
        int energy = ClientEnergyData.getEnergy();
        if (energy <= 1) {
            if (client.player.isSprinting() && !client.player.isUsingItem()) {
                client.player.setSprinting(false);
                client.options.keySprint.setDown(false);
            }
        }


        DarknessHudGuard.enforceHudVisibility();
        ErodedCompassHeartbeat.clientTick();
    }
}

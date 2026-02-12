package cz.mcsworld.eroded.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class NetworkPayloads {

    public static void registerAll() {
        PayloadTypeRegistry.playS2C().register(CraftingFailPacket.ID, CraftingFailPacket.CODEC.cast());
        PayloadTypeRegistry.playS2C().register(EnergySyncPacket.ID, EnergySyncPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(EnergyWarningPacket.ID, EnergyWarningPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(DarknessStatePacket.ID, DarknessStatePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ErodedCompassSyncPacket.ID, ErodedCompassSyncPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DodgeRequestPacket.ID, DodgeRequestPacket.CODEC);
    }

    public static void init() {
        registerAll();
    }

    private NetworkPayloads() {}
}
package cz.mcsworld.eroded.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class NetworkPayloads {

    public static void registerAll() {
        PayloadTypeRegistry.clientboundPlay().register(CraftingFailPacket.ID, CraftingFailPacket.CODEC.cast());
        PayloadTypeRegistry.clientboundPlay().register(EnergySyncPacket.ID, EnergySyncPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EnergyWarningPacket.ID, EnergyWarningPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DarknessStatePacket.ID, DarknessStatePacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ErodedCompassSyncPacket.ID, ErodedCompassSyncPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CompassDarknessBreakPacket.ID, CompassDarknessBreakPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DodgeRequestPacket.ID, DodgeRequestPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SoundTuningSyncPacket.ID, SoundTuningSyncPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HudPositionSyncPacket.ID, HudPositionSyncPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SkillSyncPacket.ID, SkillSyncPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AnvilFeedbackPacket.ID, AnvilFeedbackPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CraftingRequirementPacket.ID, CraftingRequirementPacket.CODEC);

        TerritoryModuleNetworking.registerPayloadTypes();
        TerritoryPlacementHintNetworking.registerPayloadTypes();
    }

    public static void init() {
        registerAll();
    }

    private NetworkPayloads() {}
}
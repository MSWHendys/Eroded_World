package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.protection.TerritoryClaim;
import cz.mcsworld.eroded.protection.TerritoryPermission;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class TerritoryModuleNetworking {

    private TerritoryModuleNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(
                TerritoryModuleRequestPayload.ID,
                TerritoryModuleRequestPayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                TerritoryTrustAddPayload.ID,
                TerritoryTrustAddPayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                TerritoryTrustRemovePayload.ID,
                TerritoryTrustRemovePayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                TerritoryPermissionUpdatePayload.ID,
                TerritoryPermissionUpdatePayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                TerritoryScopeUpdatePayload.ID,
                TerritoryScopeUpdatePayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                TerritorySuggestionRequestPayload.ID,
                TerritorySuggestionRequestPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                TerritoryModuleSyncPayload.ID,
                TerritoryModuleSyncPayload.CODEC
        );
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryModuleRequestPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();

                    if (!(player.getWorld() instanceof ServerWorld world)) {
                        return;
                    }

                    sendSync(world, payload.anchorPos(), player);
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryTrustAddPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity manager = context.player();

                    if (!(manager.getWorld() instanceof ServerWorld world)) {
                        return;
                    }

                    ServerPlayerEntity target = world.getServer()
                            .getPlayerManager()
                            .getPlayer(payload.playerName());

                    if (target == null) {
                        manager.sendMessage(
                                net.minecraft.text.Text.translatable(
                                        "eroded.territory.trust.player_not_found",
                                        payload.playerName()
                                ),
                                true
                        );
                        return;
                    }

                    boolean changed = TerritoryProtectionManager.addTrustedPlayer(
                            world,
                            payload.anchorPos(),
                            manager,
                            target,
                            payload.connectedArea()
                    );

                    if (changed) {
                        sendSync(world, payload.anchorPos(), manager);
                    }
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryTrustRemovePayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity manager = context.player();

                    if (!(manager.getWorld() instanceof ServerWorld world)) {
                        return;
                    }

                    UUID targetUuid;

                    try {
                        targetUuid = UUID.fromString(payload.targetUuid());
                    } catch (IllegalArgumentException ex) {
                        return;
                    }

                    boolean changed = TerritoryProtectionManager.removeTrustedPlayer(
                            world,
                            payload.anchorPos(),
                            manager,
                            targetUuid,
                            payload.targetName(),
                            payload.connectedArea()
                    );

                    if (changed) {
                        sendSync(world, payload.anchorPos(), manager);
                    }
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryPermissionUpdatePayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity manager = context.player();

                    if (!(manager.getWorld() instanceof ServerWorld world)) {
                        return;
                    }

                    UUID targetUuid;

                    try {
                        targetUuid = UUID.fromString(payload.targetUuid());
                    } catch (IllegalArgumentException ex) {
                        return;
                    }

                    TerritoryPermission permission;

                    try {
                        permission = TerritoryPermission.valueOf(payload.permissionName());
                    } catch (IllegalArgumentException ex) {
                        return;
                    }

                    boolean changed = TerritoryProtectionManager.setTrustedPermission(
                            world,
                            payload.anchorPos(),
                            manager,
                            targetUuid,
                            permission,
                            payload.enabled(),
                            payload.connectedArea()
                    );

                    if (changed) {
                        sendSync(world, payload.anchorPos(), manager);
                    }
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryScopeUpdatePayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity manager = context.player();

                    if (!(manager.getWorld() instanceof ServerWorld world)) {
                        return;
                    }

                    UUID targetUuid;

                    try {
                        targetUuid = UUID.fromString(payload.targetUuid());
                    } catch (IllegalArgumentException ex) {
                        return;
                    }

                    boolean changed = TerritoryProtectionManager.setTrustedScopeMode(
                            world,
                            payload.anchorPos(),
                            manager,
                            targetUuid,
                            payload.connectedScopeMode()
                    );

                    if (changed) {
                        sendSync(world, payload.anchorPos(), manager);
                    }
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                TerritorySuggestionRequestPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();

                    if (!(player.getWorld() instanceof ServerWorld world)) {
                        return;
                    }

                    sendSync(
                            world,
                            payload.anchorPos(),
                            player,
                            payload.query()
                    );
                })
        );
    }

    public static void sendSync(ServerWorld world, BlockPos anchorPos, ServerPlayerEntity viewer) {
        sendSync(world, anchorPos, viewer, "");
    }

    public static void sendSync(
            ServerWorld world,
            BlockPos anchorPos,
            ServerPlayerEntity viewer,
            String suggestionQuery
    ) {
        TerritoryClaim claim = TerritoryProtectionManager.getAnchorClaim(world, anchorPos);

        if (claim == null) {
            return;
        }

        if (!TerritoryProtectionManager.canManageClaim(viewer, claim)) {
            return;
        }

        TerritoryProtectionManager.ConnectedTerritoryInfo connectedInfo =
                TerritoryProtectionManager.getConnectedTerritoryInfo(world, claim);

        String trustedData = buildTrustedData(world, claim);
        String suggestionData = buildSuggestionData(world, claim, suggestionQuery);

        ServerPlayNetworking.send(
                viewer,
                new TerritoryModuleSyncPayload(
                        anchorPos,
                        claim.ownerName(),
                        claim.radius(),
                        claim.active() ? 1 : 0,
                        connectedInfo.width(),
                        connectedInfo.depth(),
                        connectedInfo.claimCount(),
                        trustedData,
                        suggestionData
                )
        );
    }

    private static String buildTrustedData(ServerWorld world, TerritoryClaim claim) {
        StringBuilder builder = new StringBuilder();

        TerritoryProtectionManager.refreshTrustedNamesFromOnlinePlayers(world, claim);

        for (TerritoryClaim.TrustedPlayer trusted : TerritoryProtectionManager.getTrustedPlayerEntries(claim)) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }

            builder.append(trusted.uuid())
                    .append("|")
                    .append(trusted.name())
                    .append("|")
                    .append(trusted.flags())
                    .append("|")
                    .append(trusted.connectedScopeMode());
        }

        return builder.toString();
    }

    private static String buildSuggestionData(ServerWorld world, TerritoryClaim claim, String query) {
        StringBuilder builder = new StringBuilder();

        for (ServerPlayerEntity player : TerritoryProtectionManager.getOnlinePlayerSuggestions(world, claim, query, 8)) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }

            builder.append(player.getUuid())
                    .append("|")
                    .append(player.getName().getString());
        }

        return builder.toString();
    }
}
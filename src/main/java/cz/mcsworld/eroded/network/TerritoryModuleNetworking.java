package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.protection.TerritoryClaim;
import cz.mcsworld.eroded.protection.TerritoryPermission;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;


public final class TerritoryModuleNetworking {

    private TerritoryModuleNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(
                TerritoryModuleRequestPayload.ID,
                TerritoryModuleRequestPayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                TerritoryTrustAddPayload.ID,
                TerritoryTrustAddPayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                TerritoryTrustRemovePayload.ID,
                TerritoryTrustRemovePayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                TerritoryPermissionUpdatePayload.ID,
                TerritoryPermissionUpdatePayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                TerritoryScopeUpdatePayload.ID,
                TerritoryScopeUpdatePayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                TerritorySuggestionRequestPayload.ID,
                TerritorySuggestionRequestPayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                TerritoryModuleSyncPayload.ID,
                TerritoryModuleSyncPayload.CODEC
        );
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryModuleRequestPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer player = context.player();

                    if (!(player.level() instanceof ServerLevel world)) {
                        return;
                    }

                    sendSync(world, payload.anchorPos(), player);
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryTrustAddPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer manager = context.player();

                    if (!(manager.level() instanceof ServerLevel world)) {
                        return;
                    }

                    ServerPlayer target = world.getServer()
                            .getPlayerList()
                            .getPlayerByName(payload.playerName());

                    if (target == null) {
                        manager.sendSystemMessage(
                                net.minecraft.network.chat.Component.translatable(
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
                    ServerPlayer manager = context.player();

                    if (!(manager.level() instanceof ServerLevel world)) {
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
                    ServerPlayer manager = context.player();

                    if (!(manager.level() instanceof ServerLevel world)) {
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
                    ServerPlayer manager = context.player();

                    if (!(manager.level() instanceof ServerLevel world)) {
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
                    ServerPlayer player = context.player();

                    if (!(player.level() instanceof ServerLevel world)) {
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

    public static void sendSync(ServerLevel world, BlockPos anchorPos, ServerPlayer viewer) {
        sendSync(world, anchorPos, viewer, "");
    }

    public static void sendSync(
            ServerLevel world,
            BlockPos anchorPos,
            ServerPlayer viewer,
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

    private static String buildTrustedData(ServerLevel world, TerritoryClaim claim) {
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

    private static String buildSuggestionData(ServerLevel world, TerritoryClaim claim, String query) {
        StringBuilder builder = new StringBuilder();

        for (ServerPlayer player : TerritoryProtectionManager.getOnlinePlayerSuggestions(world, claim, query, 8)) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }

            builder.append(player.getUUID())
                    .append("|")
                    .append(player.getName().getString());
        }

        return builder.toString();
    }
}
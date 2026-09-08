package cz.mcsworld.eroded.network;

import cz.mcsworld.eroded.protection.TerritoryClaim;
import cz.mcsworld.eroded.protection.TerritoryPermission;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;


public final class TerritoryModuleNetworking {

    private static final int UI_REQUESTS_PER_SECOND = 20;
    private static final int MUTATIONS_PER_SECOND = 12;
    private static final int SUGGESTIONS_PER_SECOND = 12;

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
        // Keep C2S work on the logical server thread. This matches the
        // Fabric 1.21.11 receiver pattern used by the original port.
        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryModuleRequestPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (!ServerPacketGuard.allow(player, "territory_ui", UI_REQUESTS_PER_SECOND)) return;
                    if (!ServerPacketGuard.validTerritoryMenu(player, payload.anchorPos())) return;
                    if (!(player.level() instanceof ServerLevel world)) return;
                    sendSync(world, payload.anchorPos(), player);
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                TerritoryTrustAddPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer manager = context.player();
                    if (!ServerPacketGuard.allow(manager, "territory_mutation", MUTATIONS_PER_SECOND)) return;
                    if (!ServerPacketGuard.validTerritoryMenu(manager, payload.anchorPos())) return;
                    if (!(manager.level() instanceof ServerLevel world)) return;

                    String playerName = payload.playerName().trim();
                    if (playerName.isEmpty() || playerName.length() > TerritoryTrustAddPayload.MAX_PLAYER_NAME_LENGTH) {
                        return;
                    }

                    ServerPlayer target = world.getServer()
                            .getPlayerList()
                            .getPlayerByName(playerName);

                    if (target == null) {
                        manager.sendSystemMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "eroded.territory.trust.player_not_found",
                                        playerName
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
                    if (!ServerPacketGuard.allow(manager, "territory_mutation", MUTATIONS_PER_SECOND)) return;
                    if (!ServerPacketGuard.validTerritoryMenu(manager, payload.anchorPos())) return;
                    if (!(manager.level() instanceof ServerLevel world)) return;

                    UUID targetUuid;
                    try {
                        targetUuid = UUID.fromString(payload.targetUuid());
                    } catch (IllegalArgumentException ex) {
                        return;
                    }

                    String targetName = payload.targetName().trim();
                    if (targetName.length() > TerritoryTrustRemovePayload.MAX_TARGET_NAME_LENGTH) {
                        return;
                    }

                    boolean changed = TerritoryProtectionManager.removeTrustedPlayer(
                            world,
                            payload.anchorPos(),
                            manager,
                            targetUuid,
                            targetName,
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
                    if (!ServerPacketGuard.allow(manager, "territory_mutation", MUTATIONS_PER_SECOND)) return;
                    if (!ServerPacketGuard.validTerritoryMenu(manager, payload.anchorPos())) return;
                    if (!(manager.level() instanceof ServerLevel world)) return;

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
                    if (!ServerPacketGuard.allow(manager, "territory_mutation", MUTATIONS_PER_SECOND)) return;
                    if (!ServerPacketGuard.validTerritoryMenu(manager, payload.anchorPos())) return;
                    if (!(manager.level() instanceof ServerLevel world)) return;

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
                    if (!ServerPacketGuard.allow(player, "territory_suggestion", SUGGESTIONS_PER_SECOND)) return;
                    if (!ServerPacketGuard.validTerritoryMenu(player, payload.anchorPos())) return;
                    if (!(player.level() instanceof ServerLevel world)) return;

                    String query = payload.query().trim();
                    if (query.length() > TerritorySuggestionRequestPayload.MAX_QUERY_LENGTH) {
                        return;
                    }

                    sendSync(world, payload.anchorPos(), player, query);
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
                        safeName(claim.ownerName(), TerritoryModuleSyncPayload.MAX_OWNER_NAME_LENGTH),
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

        int count = 0;
        for (TerritoryClaim.TrustedPlayer trusted : TerritoryProtectionManager.getTrustedPlayerEntries(claim)) {
            if (count++ >= TerritoryClaim.MAX_TRUSTED_PLAYERS) break;

            if (!builder.isEmpty()) {
                builder.append("\n");
            }

            builder.append(trusted.uuid())
                    .append("|")
                    .append(safeName(trusted.name(), TerritoryTrustAddPayload.MAX_PLAYER_NAME_LENGTH))
                    .append("|")
                    .append(trusted.flags())
                    .append("|")
                    .append(trusted.connectedScopeMode());

            if (builder.length() >= TerritoryModuleSyncPayload.MAX_TRUSTED_DATA_LENGTH - 128) break;
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
                    .append(safeName(player.getName().getString(), TerritoryTrustAddPayload.MAX_PLAYER_NAME_LENGTH));
        }

        return builder.toString();
    }
    private static String safeName(String value, int maxLength) {
        if (value == null || value.isEmpty()) return "Unknown";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

}

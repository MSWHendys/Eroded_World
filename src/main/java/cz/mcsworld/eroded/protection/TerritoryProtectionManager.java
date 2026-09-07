package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.block.TerritoryAnchorBlock;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.screen.TerritoryModuleScreenData;
import cz.mcsworld.eroded.screen.TerritoryModuleScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class TerritoryProtectionManager {

    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 256;

    /**
     * Small placement tolerance for connected player claims. Placement hints
     * mark the exact side-adjacent position, but real terrain often makes the
     * player place the next anchor one or two blocks away from that marker.
     * Keep the tolerance deliberately small so separated/diagonal territories
     * do not merge accidentally.
     */
    private static final int CLAIM_CONNECTION_GAP_TOLERANCE = 2;

    public record ConnectedTerritoryInfo(
            int width,
            int depth,
            int claimCount
    ) {
    }

    private TerritoryProtectionManager() {
    }

    private static TerritoryConfig.Server config() {
        return TerritoryConfig.get().server;
    }

    public static boolean isPlayerClaimProtectionEnabled() {
        return config().playerClaimProtectionEnabled;
    }

    public static int getConfiguredRadius() {
        int radius = config().playerClaimRadius;
        return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
    }

    public static int getActivationDelayTicks() {
        int seconds = Math.max(1, config().playerClaimActivationDelaySeconds);
        return seconds * 20;
    }

    public static int getMaxClaimsPerPlayer() {
        return config().maxClaimsPerPlayer;
    }

    public static boolean canBypass(ServerPlayer player) {
        return config().claimCreativeBypass && player.isCreative();
    }

    public static void createPendingClaim(ServerLevel world, BlockPos anchorPos, ServerPlayer owner) {
        if (!isPlayerClaimProtectionEnabled()) {
            return;
        }

        TerritoryClaimState state = TerritoryClaimState.get(world);

        TerritoryClaim claim = new TerritoryClaim(
                anchorPos,
                owner.getUUID(),
                owner.getName().getString(),
                getConfiguredRadius(),
                false
        );

        state.put(claim);
    }

    public static void activateClaim(ServerLevel world, BlockPos anchorPos) {
        if (!isPlayerClaimProtectionEnabled()) {
            return;
        }

        TerritoryClaimState state = TerritoryClaimState.get(world);
        TerritoryClaim claim = state.getByAnchor(anchorPos);

        if (claim == null) {
            return;
        }

        claim.setActive(true);
        state.setDirty();
    }

    public static TerritoryClaim removeClaim(ServerLevel world, BlockPos anchorPos) {
        return TerritoryClaimState.get(world).remove(anchorPos);
    }

    public static TerritoryClaim getAnchorClaim(ServerLevel world, BlockPos pos) {
        TerritoryClaimState state = TerritoryClaimState.get(world);
        TerritoryClaim claim = state.getByAnchor(pos);

        if (claim == null) {
            return null;
        }

        if (isClaimStillValid(world, claim)) {
            return claim;
        }

        state.remove(pos);
        return null;
    }

    public static TerritoryClaim getActiveClaimAt(ServerLevel world, BlockPos pos) {
        TerritoryClaimState state = TerritoryClaimState.get(world);

        while (true) {
            TerritoryClaim claim = state.findActiveAt(pos);

            if (claim == null) {
                return null;
            }

            if (isClaimStillValid(world, claim)) {
                return claim;
            }

            state.remove(claim.anchorPos());
        }
    }

    public static int getClaimCount(ServerLevel world, ServerPlayer player) {
        TerritoryClaimState state = TerritoryClaimState.get(world);
        pruneInvalidLoadedClaims(world, state);
        return state.countByOwner(player.getUUID());
    }

    public static boolean canCreateMoreClaims(ServerLevel world, ServerPlayer player) {
        if (canBypass(player)) {
            return true;
        }

        int maxClaims = getMaxClaimsPerPlayer();

        if (maxClaims <= 0) {
            return true;
        }

        return getClaimCount(world, player) < maxClaims;
    }

    public static TerritoryClaim getOverlappingClaim(ServerLevel world, BlockPos anchorPos, int radius) {
        if (!config().preventPlayerClaimOverlap) {
            return null;
        }

        TerritoryClaimState state = TerritoryClaimState.get(world);
        pruneInvalidLoadedClaims(world, state);
        return state.findOverlappingClaim(anchorPos, radius);
    }

    public static boolean validateNewClaim(ServerLevel world, BlockPos anchorPos, ServerPlayer player) {
        if (!isPlayerClaimProtectionEnabled()) {
            return true;
        }

        if (!canCreateMoreClaims(world, player)) {
            sendMaxClaimsMessage(
                    player,
                    getClaimCount(world, player),
                    getMaxClaimsPerPlayer()
            );
            return false;
        }

        TerritoryClaim overlappingClaim = getOverlappingClaim(
                world,
                anchorPos,
                getConfiguredRadius()
        );

        if (overlappingClaim != null) {
            sendOverlapMessage(player, overlappingClaim);
            return false;
        }

        return true;
    }

    public static boolean canBreakBlock(ServerPlayer player, ServerLevel world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled() || !config().protectClaimBlockBreak) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim anchorClaim = getAnchorClaim(world, pos);

        if (anchorClaim != null) {
            return anchorClaim.isOwner(player);
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.BREAK);
    }

    public static boolean canPlaceBlock(ServerPlayer player, ServerLevel world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled() || !config().protectClaimBlockPlace) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.BUILD);
    }

    public static boolean canOpenContainer(ServerPlayer player, ServerLevel world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled() || !config().protectClaimContainers) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.CONTAINERS);
    }

    public static boolean canUseFire(ServerPlayer player, ServerLevel world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled() || !config().protectClaimFire) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.FIRE);
    }

    public static boolean canUseRedstone(ServerPlayer player, ServerLevel world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled()) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.REDSTONE);
    }

    public static boolean canModifyEntity(ServerPlayer player, ServerLevel world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled()) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.ENTITIES);
    }

    public static boolean isExplosionProtected(ServerLevel world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled() || !config().protectClaimExplosions) {
            return false;
        }

        TerritoryClaim anchorClaim = getAnchorClaim(world, pos);

        if (anchorClaim != null) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim != null;
    }

    private static boolean hasClaimPermission(
            ServerPlayer player,
            TerritoryClaim claim,
            TerritoryPermission permission
    ) {
        if (claim == null) {
            return true;
        }

        return claim.hasPermission(player, permission);
    }

    public static void giveModuleBack(Player player) {
        ItemStack module = new ItemStack(ErodedItems.TERRITORY_MODULE);

        if (!player.getInventory().add(module)) {
            player.drop(module, false);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            syncInventory(serverPlayer);
        }
    }

    public static void handleAnchorBroken(
            ServerLevel world,
            BlockPos pos,
            BlockState state,
            ServerPlayer player
    ) {
        TerritoryClaim removed = removeClaim(world, pos);

        if (removed == null) {
            return;
        }

        boolean hadModule = state.hasProperty(TerritoryAnchorBlock.HAS_MODULE)
                && state.getValue(TerritoryAnchorBlock.HAS_MODULE);

        if (hadModule && removed.isOwner(player) && !player.isCreative()) {
            giveModuleBack(player);
        }

        player.displayClientMessage(
                Component.translatable("eroded.territory.anchor.removed"),
                true
        );
    }

    public static void sendProtectedMessage(ServerPlayer player) {
        playActionDeniedSound(player);

        player.displayClientMessage(
                Component.translatable("eroded.territory.protected"),
                true
        );
    }

    public static void sendAnchorOwnerOnlyMessage(ServerPlayer player) {
        playActionDeniedSound(player);

        player.displayClientMessage(
                Component.translatable("eroded.territory.anchor.owner_only"),
                true
        );
    }

    public static void sendMaxClaimsMessage(ServerPlayer player, int current, int max) {
        playActionDeniedSound(player);

        player.displayClientMessage(
                Component.translatable(
                        "eroded.territory.anchor.max_claims",
                        current,
                        max
                ),
                true
        );
    }

    public static void sendOverlapMessage(ServerPlayer player, TerritoryClaim overlappingClaim) {
        playActionDeniedSound(player);

        if (overlappingClaim.isOwner(player)) {
            player.displayClientMessage(
                    Component.translatable("eroded.territory.anchor.overlap_own"),
                    true
            );
            return;
        }

        player.displayClientMessage(
                Component.translatable(
                        "eroded.territory.anchor.overlap_other",
                        overlappingClaim.ownerName()
                ),
                true
        );
    }

    public static boolean canManageClaim(ServerPlayer player, TerritoryClaim claim) {
        if (claim == null) {
            return false;
        }

        return canBypass(player) || claim.isOwner(player);
    }

    public static void openTerritoryModule(
            ServerLevel world,
            BlockPos anchorPos,
            ServerPlayer player
    ) {
        TerritoryClaim claim = getAnchorClaim(world, anchorPos);

        if (claim == null) {
            player.displayClientMessage(
                    Component.translatable("eroded.territory.trust.no_claim"),
                    true
            );
            return;
        }

        if (!canManageClaim(player, claim)) {
            player.displayClientMessage(
                    Component.translatable("eroded.territory.trust.no_permission"),
                    true
            );
            return;
        }

        TerritoryModuleScreenData data = new TerritoryModuleScreenData(anchorPos);

        playTerritoryModuleOpenSound(player);

        player.openMenu(new ExtendedScreenHandlerFactory<TerritoryModuleScreenData>() {
            @Override
            public TerritoryModuleScreenData getScreenOpeningData(ServerPlayer player) {
                return data;
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.eroded.territory_module");
            }

            @Override
            public AbstractContainerMenu createMenu(
                    int syncId,
                    Inventory playerInventory,
                    Player player
            ) {
                return new TerritoryModuleScreenHandler(
                        syncId,
                        playerInventory,
                        data
                );
            }
        });
    }

    public static boolean addTrustedPlayer(
            ServerLevel world,
            BlockPos anchorPos,
            ServerPlayer manager,
            ServerPlayer target,
            boolean connectedArea
    ) {
        TerritoryClaim claim = getAnchorClaim(world, anchorPos);

        if (claim == null) {
            claim = getActiveClaimAt(world, anchorPos);
        }

        if (claim == null) {
            manager.displayClientMessage(
                    Component.translatable("eroded.territory.trust.no_claim"),
                    true
            );
            return false;
        }

        if (!canManageClaim(manager, claim)) {
            manager.displayClientMessage(
                    Component.translatable("eroded.territory.trust.no_permission"),
                    true
            );
            return false;
        }

        if (claim.isOwner(target)) {
            manager.displayClientMessage(
                    Component.translatable("eroded.territory.trust.owner"),
                    true
            );
            return false;
        }

        List<TerritoryClaim> targetClaims = connectedArea
                ? findConnectedClaims(world, claim)
                : List.of(claim);

        boolean changed = false;
        boolean trustLimitReached = false;

        for (TerritoryClaim targetClaim : targetClaims) {
            if (!targetClaim.canAddTrusted(target.getUUID())) {
                trustLimitReached = true;
                continue;
            }

            if (targetClaim.addTrusted(
                    target.getUUID(),
                    target.getName().getString()
            )) {
                changed = true;
            }

            if (connectedArea
                    && targetClaim.setTrustedScopeMode(target.getUUID(), true)) {
                changed = true;
            }
        }

        if (!changed) {
            manager.displayClientMessage(
                    Component.translatable(
                            trustLimitReached
                                    ? "eroded.territory.trust.limit"
                                    : "eroded.territory.trust.already",
                            trustLimitReached
                                    ? TerritoryClaim.MAX_TRUSTED_PLAYERS
                                    : target.getName().getString()
                    ),
                    true
            );
            return false;
        }

        if (trustLimitReached) {
            manager.displayClientMessage(
                    Component.translatable(
                            "eroded.territory.trust.limit_partial",
                            TerritoryClaim.MAX_TRUSTED_PLAYERS
                    ),
                    true
            );
        }

        TerritoryClaimState.get(world).setDirty();

        if (!trustLimitReached) {
            manager.displayClientMessage(
                    Component.translatable(
                            connectedArea
                                    ? "eroded.territory.trust.added.connected"
                                    : "eroded.territory.trust.added",
                            target.getName().getString()
                    ),
                    true
            );
        }

        target.displayClientMessage(
                Component.translatable(
                        "eroded.territory.trust.added.target",
                        claim.ownerName()
                ),
                true
        );

        playTrustAddedSound(manager);
        playTrustAddedSound(target);

        return true;
    }

    public static boolean setTrustedPermission(
            ServerLevel world,
            BlockPos anchorPos,
            ServerPlayer manager,
            UUID targetUuid,
            TerritoryPermission permission,
            boolean enabled,
            boolean connectedArea
    ) {
        TerritoryClaim claim = getAnchorClaim(world, anchorPos);

        if (claim == null) {
            claim = getActiveClaimAt(world, anchorPos);
        }

        if (claim == null) {
            manager.displayClientMessage(
                    Component.translatable("eroded.territory.trust.no_claim"),
                    true
            );
            return false;
        }

        if (!canManageClaim(manager, claim)) {
            manager.displayClientMessage(
                    Component.translatable("eroded.territory.trust.no_permission"),
                    true
            );
            return false;
        }

        if (targetUuid == null || permission == null) {
            return false;
        }

        if (claim.ownerUuid().equals(targetUuid)) {
            return false;
        }

        List<TerritoryClaim> targetClaims = connectedArea
                ? findConnectedClaims(world, claim)
                : List.of(claim);

        boolean changed = false;
        boolean trustLimitReached = false;
        TerritoryClaim.TrustedPlayer sourceAccess = connectedArea
                ? claim.getTrustedPlayer(targetUuid)
                : null;

        for (TerritoryClaim targetClaim : targetClaims) {
            // Self-heal saves created by older versions where the UI stored
            // connectedScopeMode=true only on the current anchor. Before an
            // area-wide permission change, make sure the trusted entry exists
            // everywhere and carries the current anchor's permission mask.
            if (connectedArea && sourceAccess != null) {
                if (!targetClaim.canAddTrusted(targetUuid)) {
                    trustLimitReached = true;
                    continue;
                }

                if (targetClaim.syncTrustedAccess(
                        targetUuid,
                        sourceAccess.name(),
                        sourceAccess.flags(),
                        true
                )) {
                    changed = true;
                }
            }

            if (targetClaim.setTrustedPermission(
                    targetUuid,
                    permission,
                    enabled
            )) {
                changed = true;
            }
        }

        if (trustLimitReached) {
            manager.displayClientMessage(
                    Component.translatable(
                            "eroded.territory.trust.limit_partial",
                            TerritoryClaim.MAX_TRUSTED_PLAYERS
                    ),
                    true
            );
        }

        if (changed) {
            TerritoryClaimState.get(world).setDirty();
        }

        return changed;
    }

    public static boolean setTrustedScopeMode(
            ServerLevel world,
            BlockPos anchorPos,
            ServerPlayer manager,
            UUID targetUuid,
            boolean connectedScopeMode
    ) {
        TerritoryClaim claim = getAnchorClaim(world, anchorPos);

        if (claim == null) {
            claim = getActiveClaimAt(world, anchorPos);
        }

        if (claim == null) {
            manager.displayClientMessage(
                    Component.translatable("eroded.territory.trust.no_claim"),
                    true
            );
            return false;
        }

        if (!canManageClaim(manager, claim)) {
            manager.displayClientMessage(
                    Component.translatable("eroded.territory.trust.no_permission"),
                    true
            );
            return false;
        }

        if (targetUuid == null || claim.ownerUuid().equals(targetUuid)) {
            return false;
        }

        TerritoryClaim.TrustedPlayer sourceAccess = claim.getTrustedPlayer(targetUuid);

        if (sourceAccess == null) {
            return false;
        }

        List<TerritoryClaim> connectedClaims = findConnectedClaims(world, claim);
        boolean changed = false;
        boolean trustLimitReached = false;

        if (connectedScopeMode) {
            // "All connected anchors" is not only a UI flag. The trusted
            // player must exist on every connected claim; otherwise later
            // area-wide permission updates have nothing to modify there. Copy
            // the current anchor's permission mask to every connected anchor.
            for (TerritoryClaim targetClaim : connectedClaims) {
                if (!targetClaim.canAddTrusted(targetUuid)) {
                    trustLimitReached = true;
                    continue;
                }

                if (targetClaim.syncTrustedAccess(
                        targetUuid,
                        sourceAccess.name(),
                        sourceAccess.flags(),
                        true
                )) {
                    changed = true;
                }
            }
        } else {
            // Scope mode is a property of the connected management set. Keep
            // it consistent on every anchor where the player is already
            // trusted, but do not revoke trust from the other anchors.
            for (TerritoryClaim targetClaim : connectedClaims) {
                if (targetClaim.setTrustedScopeMode(targetUuid, false)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            TerritoryClaimState.get(world).setDirty();
        }

        if (trustLimitReached) {
            manager.displayClientMessage(
                    Component.translatable(
                            "eroded.territory.trust.limit_partial",
                            TerritoryClaim.MAX_TRUSTED_PLAYERS
                    ),
                    true
            );
        }

        return changed;
    }

    public static boolean addTrustedPlayer(
            ServerLevel world,
            BlockPos anchorPos,
            ServerPlayer manager,
            ServerPlayer target
    ) {
        return addTrustedPlayer(world, anchorPos, manager, target, false);
    }

    public static boolean removeTrustedPlayer(
            ServerLevel world,
            BlockPos anchorPos,
            ServerPlayer manager,
            UUID targetUuid,
            String targetName,
            boolean connectedArea
    ) {
        TerritoryClaim claim = getAnchorClaim(world, anchorPos);

        if (claim == null) {
            claim = getActiveClaimAt(world, anchorPos);
        }

        if (claim == null) {
            manager.displayClientMessage(
                    Component.translatable("eroded.territory.trust.no_claim"),
                    true
            );
            return false;
        }

        if (!canManageClaim(manager, claim)) {
            manager.displayClientMessage(
                    Component.translatable("eroded.territory.trust.no_permission"),
                    true
            );
            return false;
        }

        List<TerritoryClaim> targetClaims = connectedArea
                ? findConnectedClaims(world, claim)
                : List.of(claim);

        boolean removed = false;

        for (TerritoryClaim targetClaim : targetClaims) {
            if (targetClaim.removeTrusted(targetUuid)) {
                removed = true;
            }
        }

        if (!removed) {
            manager.displayClientMessage(
                    Component.translatable(
                            "eroded.territory.trust.not_trusted",
                            targetName
                    ),
                    true
            );
            return false;
        }

        TerritoryClaimState.get(world).setDirty();

        manager.displayClientMessage(
                Component.translatable(
                        connectedArea
                                ? "eroded.territory.trust.removed.connected"
                                : "eroded.territory.trust.removed",
                        targetName
                ),
                true
        );

        ServerPlayer target = world.getServer()
                .getPlayerList()
                .getPlayer(targetUuid);

        if (target != null) {
            target.displayClientMessage(
                    Component.translatable(
                            "eroded.territory.trust.removed.target",
                            claim.ownerName()
                    ),
                    true
            );

            playTrustRemovedSound(target);
        }

        playTrustRemovedSound(manager);

        return true;
    }

    public static boolean removeTrustedPlayer(
            ServerLevel world,
            BlockPos anchorPos,
            ServerPlayer manager,
            UUID targetUuid,
            String targetName
    ) {
        return removeTrustedPlayer(world, anchorPos, manager, targetUuid, targetName, false);
    }

    public static List<ServerPlayer> getOnlinePlayerSuggestions(
            ServerLevel world,
            TerritoryClaim claim,
            String query,
            int maxSuggestions
    ) {
        List<ServerPlayer> suggestions = new ArrayList<>();

        if (claim == null || maxSuggestions <= 0) {
            return suggestions;
        }

        String normalizedQuery = query == null
                ? ""
                : query.trim().toLowerCase(Locale.ROOT);

        for (ServerPlayer player : world.getServer().getPlayerList().getPlayers()) {
            if (claim.isOwner(player)) {
                continue;
            }

            if (claim.isTrusted(player)) {
                continue;
            }

            String playerName = player.getName().getString();

            if (!normalizedQuery.isBlank()
                    && !playerName.toLowerCase(Locale.ROOT).startsWith(normalizedQuery)) {
                continue;
            }

            suggestions.add(player);

            if (suggestions.size() >= maxSuggestions) {
                break;
            }
        }

        return suggestions;
    }

    public static Set<TerritoryClaim.TrustedPlayer> getTrustedPlayerEntries(TerritoryClaim claim) {
        if (claim == null) {
            return Set.of();
        }

        return claim.trustedPlayerEntries();
    }

    public static void refreshTrustedNamesFromOnlinePlayers(ServerLevel world, TerritoryClaim claim) {
        if (claim == null) {
            return;
        }

        boolean changed = false;

        for (ServerPlayer player : world.getServer().getPlayerList().getPlayers()) {
            if (claim.isTrusted(player)) {
                if (claim.updateTrustedName(
                        player.getUUID(),
                        player.getName().getString()
                )) {
                    changed = true;
                }
            }
        }

        if (changed) {
            TerritoryClaimState.get(world).setDirty();
        }
    }

    public static void playAnchorActivatedSound(ServerLevel world, BlockPos pos) {
        world.playSound(
                null,
                pos,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                0.8F,
                1.15F
        );

        world.playSound(
                null,
                pos,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS,
                0.7F,
                1.35F
        );
    }

    public static void playModuleInsertedSound(ServerLevel world, BlockPos pos) {
        world.playSound(
                null,
                pos,
                SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS,
                0.8F,
                1.15F
        );

        world.playSound(
                null,
                pos,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS,
                0.45F,
                1.65F
        );
    }

    private static void playTrustAddedSound(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.55F,
                1.25F
        );
    }

    private static void playTrustRemovedSound(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS,
                0.35F,
                1.65F
        );
    }

    private static void playActionDeniedSound(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.NOTE_BLOCK_BASS.value(),
                SoundSource.PLAYERS,
                0.45F,
                0.7F
        );
    }

    private static void playTerritoryModuleOpenSound(ServerPlayer player) {
        player.playNotifySound(
                SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS,
                0.8F,
                1.25F
        );

        player.playNotifySound(
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.55F,
                1.45F
        );
    }

    private static void playPlacementHintActivatedSound(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS,
                0.55F,
                1.35F
        );

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.35F,
                1.75F
        );
    }

    public static void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.sendAllDataToRemote();
    }

    private static TerritoryClaim findNearestActiveClaimOwnedBy(
            ServerLevel world,
            ServerPlayer player
    ) {
        TerritoryClaimState state = TerritoryClaimState.get(world);
        pruneInvalidLoadedClaims(world, state);

        TerritoryClaim nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (TerritoryClaim claim : state.all()) {
            if (!claim.active()) {
                continue;
            }

            if (!claim.ownerUuid().equals(player.getUUID())) {
                continue;
            }

            BlockPos anchorPos = claim.anchorPos();

            double dx = player.getX() - (anchorPos.getX() + 0.5);
            double dz = player.getZ() - (anchorPos.getZ() + 0.5);
            double distance = dx * dx + dz * dz;

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = claim;
            }
        }

        return nearest;
    }

    public static ConnectedTerritoryInfo getConnectedTerritoryInfo(
            ServerLevel world,
            TerritoryClaim startClaim
    ) {
        List<TerritoryClaim> connectedClaims = findConnectedClaims(world, startClaim);

        if (connectedClaims.isEmpty()) {
            return new ConnectedTerritoryInfo(0, 0, 0);
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (TerritoryClaim claim : connectedClaims) {
            minX = Math.min(minX, claimMinX(claim));
            maxX = Math.max(maxX, claimMaxX(claim));
            minZ = Math.min(minZ, claimMinZ(claim));
            maxZ = Math.max(maxZ, claimMaxZ(claim));
        }

        return new ConnectedTerritoryInfo(
                maxX - minX + 1,
                maxZ - minZ + 1,
                connectedClaims.size()
        );
    }

    public static List<TerritoryClaim> findConnectedClaims(
            ServerLevel world,
            TerritoryClaim startClaim
    ) {
        List<TerritoryClaim> result = new ArrayList<>();

        if (startClaim == null || !startClaim.active()) {
            return result;
        }

        TerritoryClaimState state = TerritoryClaimState.get(world);
        pruneInvalidLoadedClaims(world, state);

        TerritoryClaim persistedStart = state.getByAnchor(startClaim.anchorPos());

        if (persistedStart == null || !persistedStart.active()) {
            return result;
        }

        // One stable snapshot for the whole traversal. Besides avoiding live-map
        // mutation hazards, this also avoids allocating a new claims snapshot
        // for every BFS node. A spatial index is a separate performance task.
        List<TerritoryClaim> allClaims = state.all();

        HashSet<BlockPos> visited = new HashSet<>();
        Queue<TerritoryClaim> queue = new ArrayDeque<>();

        queue.add(persistedStart);
        visited.add(persistedStart.anchorPos());

        while (!queue.isEmpty()) {
            TerritoryClaim current = queue.poll();
            result.add(current);

            for (TerritoryClaim candidate : allClaims) {
                if (candidate == current) {
                    continue;
                }

                if (!candidate.active()) {
                    continue;
                }

                if (!candidate.ownerUuid().equals(persistedStart.ownerUuid())) {
                    continue;
                }

                if (visited.contains(candidate.anchorPos())) {
                    continue;
                }

                if (!areClaimsConnectedBySide(current, candidate)) {
                    continue;
                }

                visited.add(candidate.anchorPos());
                queue.add(candidate);
            }
        }

        return result;
    }

    /**
     * A claim in an unloaded chunk is retained without forcing that chunk to
     * load. Once its anchor chunk is loaded, the block is authoritative and a
     * missing anchor makes the persisted claim stale.
     */
    private static boolean isClaimStillValid(ServerLevel world, TerritoryClaim claim) {
        BlockPos anchorPos = claim.anchorPos();

        if (!world.hasChunkAt(anchorPos)) {
            return true;
        }

        return world.getBlockState(anchorPos).is(ErodedBlocks.TERRITORY_ANCHOR);
    }

    /**
     * Removes only claims whose anchor chunks are already loaded and whose
     * anchor blocks are gone. This cleans ghost claims without causing chunk
     * loads merely because a protection lookup happened elsewhere.
     */
    private static void pruneInvalidLoadedClaims(
            ServerLevel world,
            TerritoryClaimState state
    ) {
        List<BlockPos> staleAnchors = new ArrayList<>();

        for (TerritoryClaim claim : state.all()) {
            if (!isClaimStillValid(world, claim)) {
                staleAnchors.add(claim.anchorPos());
            }
        }

        for (BlockPos staleAnchor : staleAnchors) {
            state.remove(staleAnchor);
        }
    }

    private static boolean areClaimsConnectedBySide(TerritoryClaim a, TerritoryClaim b) {
        int aMinX = claimMinX(a);
        int aMaxX = claimMaxX(a);
        int aMinZ = claimMinZ(a);
        int aMaxZ = claimMaxZ(a);

        int bMinX = claimMinX(b);
        int bMaxX = claimMaxX(b);
        int bMinZ = claimMinZ(b);
        int bMaxZ = claimMaxZ(b);

        boolean overlapsOnX = rangesOverlap(aMinX, aMaxX, bMinX, bMaxX);
        boolean overlapsOnZ = rangesOverlap(aMinZ, aMaxZ, bMinZ, bMaxZ);

        // If overlap protection is disabled, two overlapping claims owned by
        // the same player still form one connected territory.
        if (overlapsOnX && overlapsOnZ) {
            return true;
        }

        int gapOnX = rangeGap(aMinX, aMaxX, bMinX, bMaxX);
        int gapOnZ = rangeGap(aMinZ, aMaxZ, bMinZ, bMaxZ);

        // Side connection only: the perpendicular ranges must overlap. This
        // intentionally prevents diagonal/corner-only claims from connecting.
        if (overlapsOnZ && gapOnX <= CLAIM_CONNECTION_GAP_TOLERANCE) {
            return true;
        }

        return overlapsOnX && gapOnZ <= CLAIM_CONNECTION_GAP_TOLERANCE;
    }

    private static boolean rangesOverlap(int aMin, int aMax, int bMin, int bMax) {
        return aMin <= bMax && bMin <= aMax;
    }

    /**
     * Number of completely unclaimed blocks between two inclusive ranges.
     * Returns 0 for overlapping or directly adjacent ranges.
     */
    private static int rangeGap(int aMin, int aMax, int bMin, int bMax) {
        if (rangesOverlap(aMin, aMax, bMin, bMax)) {
            return 0;
        }

        if (aMax < bMin) {
            return Math.max(0, bMin - aMax - 1);
        }

        return Math.max(0, aMin - bMax - 1);
    }

    private static int claimMinX(TerritoryClaim claim) {
        return claim.anchorPos().getX() - claim.radius();
    }

    private static int claimMaxX(TerritoryClaim claim) {
        return claim.anchorPos().getX() + claim.radius();
    }

    private static int claimMinZ(TerritoryClaim claim) {
        return claim.anchorPos().getZ() - claim.radius();
    }

    private static int claimMaxZ(TerritoryClaim claim) {
        return claim.anchorPos().getZ() + claim.radius();
    }

    public static void showNextAnchorSuggestions(
            ServerLevel world,
            ServerPlayer player,
            boolean showMessage
    ) {
        TerritoryClaim claim = findNearestActiveClaimOwnedBy(world, player);

        if (claim == null) {
            if (showMessage) {
                player.displayClientMessage(
                        Component.translatable("eroded.territory.hint.no_active_claim"),
                        true
                );
            }

            return;
        }

        int spacing = claim.radius() + getConfiguredRadius() + 1;
        BlockPos anchor = claim.anchorPos();

        BlockPos[] suggestedPositions = new BlockPos[]{
                anchor.offset(spacing, 0, 0),
                anchor.offset(-spacing, 0, 0),
                anchor.offset(0, 0, spacing),
                anchor.offset(0, 0, -spacing)
        };

        for (BlockPos suggestedPos : suggestedPositions) {
            spawnSuggestionMarker(world, player, suggestedPos);
        }

        if (showMessage) {
            playPlacementHintActivatedSound(player);

            player.displayClientMessage(
                    Component.translatable(
                            "eroded.territory.hint.shown",
                            spacing
                    ),
                    true
            );
        }
    }

    private static void spawnSuggestionMarker(
            ServerLevel world,
            ServerPlayer player,
            BlockPos pos
    ) {
        // A placement hint is visual-only. Never force-load a remote chunk just
        // to discover its surface height; if the suggested area is not loaded,
        // skip this marker until the player actually approaches it.
        BlockPos probe = new BlockPos(pos.getX(), player.blockPosition().getY(), pos.getZ());
        if (!world.hasChunkAt(probe)) {
            return;
        }

        int surfaceY = world.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(),
                pos.getZ()
        );

        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;

        for (int i = 0; i < 8; i++) {
            double y = surfaceY + 0.15 + (i * 0.3);

            world.sendParticles(
                    player,
                    ParticleTypes.END_ROD,
                    true,
                    false,
                    x,
                    y,
                    z,
                    6,
                    0.10,
                    0.03,
                    0.10,
                    0.01
            );
        }

        world.sendParticles(
                player,
                ParticleTypes.HAPPY_VILLAGER,
                true,
                false,
                x,
                surfaceY + 1.05,
                z,
                10,
                0.22,
                0.10,
                0.22,
                0.02
        );
    }
}
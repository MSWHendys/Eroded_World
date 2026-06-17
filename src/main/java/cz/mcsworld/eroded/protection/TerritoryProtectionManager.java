package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.block.TerritoryAnchorBlock;
import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.screen.TerritoryModuleScreenData;
import cz.mcsworld.eroded.screen.TerritoryModuleScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

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

    public static boolean canBypass(ServerPlayerEntity player) {
        return config().claimCreativeBypass && player.isCreative();
    }

    public static void createPendingClaim(ServerWorld world, BlockPos anchorPos, ServerPlayerEntity owner) {
        if (!isPlayerClaimProtectionEnabled()) {
            return;
        }

        TerritoryClaimState state = TerritoryClaimState.get(world);

        TerritoryClaim claim = new TerritoryClaim(
                anchorPos,
                owner.getUuid(),
                owner.getName().getString(),
                getConfiguredRadius(),
                false
        );

        state.put(claim);
    }

    public static void activateClaim(ServerWorld world, BlockPos anchorPos) {
        if (!isPlayerClaimProtectionEnabled()) {
            return;
        }

        TerritoryClaimState state = TerritoryClaimState.get(world);
        TerritoryClaim claim = state.getByAnchor(anchorPos);

        if (claim == null) {
            return;
        }

        claim.setActive(true);
        state.markDirty();
    }

    public static TerritoryClaim removeClaim(ServerWorld world, BlockPos anchorPos) {
        return TerritoryClaimState.get(world).remove(anchorPos);
    }

    public static TerritoryClaim getAnchorClaim(ServerWorld world, BlockPos pos) {
        return TerritoryClaimState.get(world).getByAnchor(pos);
    }

    public static TerritoryClaim getActiveClaimAt(ServerWorld world, BlockPos pos) {
        TerritoryClaimState state = TerritoryClaimState.get(world);

        while (true) {
            TerritoryClaim claim = state.findActiveAt(pos);

            if (claim == null) {
                return null;
            }

            if (world.getBlockState(claim.anchorPos()).isOf(ErodedBlocks.TERRITORY_ANCHOR)) {
                return claim;
            }

            state.remove(claim.anchorPos());
        }
    }

    public static int getClaimCount(ServerWorld world, ServerPlayerEntity player) {
        return TerritoryClaimState.get(world).countByOwner(player.getUuid());
    }

    public static boolean canCreateMoreClaims(ServerWorld world, ServerPlayerEntity player) {
        if (canBypass(player)) {
            return true;
        }

        int maxClaims = getMaxClaimsPerPlayer();

        if (maxClaims <= 0) {
            return true;
        }

        return getClaimCount(world, player) < maxClaims;
    }

    public static TerritoryClaim getOverlappingClaim(ServerWorld world, BlockPos anchorPos, int radius) {
        if (!config().preventPlayerClaimOverlap) {
            return null;
        }

        return TerritoryClaimState.get(world).findOverlappingClaim(anchorPos, radius);
    }

    public static boolean validateNewClaim(ServerWorld world, BlockPos anchorPos, ServerPlayerEntity player) {
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

    public static boolean canBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
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

    public static boolean canPlaceBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled() || !config().protectClaimBlockPlace) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.BUILD);
    }

    public static boolean canOpenContainer(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled() || !config().protectClaimContainers) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.CONTAINERS);
    }

    public static boolean canUseFire(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled() || !config().protectClaimFire) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.FIRE);
    }

    public static boolean canUseRedstone(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled()) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.REDSTONE);
    }

    public static boolean canModifyEntity(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!isPlayerClaimProtectionEnabled()) {
            return true;
        }

        if (canBypass(player)) {
            return true;
        }

        TerritoryClaim claim = getActiveClaimAt(world, pos);

        return claim == null || hasClaimPermission(player, claim, TerritoryPermission.ENTITIES);
    }

    public static boolean isExplosionProtected(ServerWorld world, BlockPos pos) {
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
            ServerPlayerEntity player,
            TerritoryClaim claim,
            TerritoryPermission permission
    ) {
        if (claim == null) {
            return true;
        }

        return claim.hasPermission(player, permission);
    }

    public static void giveModuleBack(PlayerEntity player) {
        ItemStack module = new ItemStack(ErodedItems.TERRITORY_MODULE);

        if (!player.getInventory().insertStack(module)) {
            player.dropItem(module, false);
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            syncInventory(serverPlayer);
        }
    }

    public static void handleAnchorBroken(
            ServerWorld world,
            BlockPos pos,
            BlockState state,
            ServerPlayerEntity player
    ) {
        TerritoryClaim removed = removeClaim(world, pos);

        if (removed == null) {
            return;
        }

        boolean hadModule = state.contains(TerritoryAnchorBlock.HAS_MODULE)
                && state.get(TerritoryAnchorBlock.HAS_MODULE);

        if (hadModule && removed.isOwner(player) && !player.isCreative()) {
            giveModuleBack(player);
        }

        player.sendMessage(
                Text.translatable("eroded.territory.anchor.removed"),
                true
        );
    }

    public static void sendProtectedMessage(ServerPlayerEntity player) {
        playActionDeniedSound(player);

        player.sendMessage(
                Text.translatable("eroded.territory.protected"),
                true
        );
    }

    public static void sendAnchorOwnerOnlyMessage(ServerPlayerEntity player) {
        playActionDeniedSound(player);

        player.sendMessage(
                Text.translatable("eroded.territory.anchor.owner_only"),
                true
        );
    }

    public static void sendMaxClaimsMessage(ServerPlayerEntity player, int current, int max) {
        playActionDeniedSound(player);

        player.sendMessage(
                Text.translatable(
                        "eroded.territory.anchor.max_claims",
                        current,
                        max
                ),
                true
        );
    }

    public static void sendOverlapMessage(ServerPlayerEntity player, TerritoryClaim overlappingClaim) {
        playActionDeniedSound(player);

        if (overlappingClaim.isOwner(player)) {
            player.sendMessage(
                    Text.translatable("eroded.territory.anchor.overlap_own"),
                    true
            );
            return;
        }

        player.sendMessage(
                Text.translatable(
                        "eroded.territory.anchor.overlap_other",
                        overlappingClaim.ownerName()
                ),
                true
        );
    }

    public static boolean canManageClaim(ServerPlayerEntity player, TerritoryClaim claim) {
        if (claim == null) {
            return false;
        }

        return canBypass(player) || claim.isOwner(player);
    }

    public static void openTerritoryModule(
            ServerWorld world,
            BlockPos anchorPos,
            ServerPlayerEntity player
    ) {
        TerritoryClaim claim = getAnchorClaim(world, anchorPos);

        if (claim == null) {
            player.sendMessage(
                    Text.translatable("eroded.territory.trust.no_claim"),
                    true
            );
            return;
        }

        if (!canManageClaim(player, claim)) {
            player.sendMessage(
                    Text.translatable("eroded.territory.trust.no_permission"),
                    true
            );
            return;
        }

        TerritoryModuleScreenData data = new TerritoryModuleScreenData(anchorPos);

        playTerritoryModuleOpenSound(player);

        player.openHandledScreen(new ExtendedScreenHandlerFactory<TerritoryModuleScreenData>() {
            @Override
            public TerritoryModuleScreenData getScreenOpeningData(ServerPlayerEntity player) {
                return data;
            }

            @Override
            public Text getDisplayName() {
                return Text.translatable("screen.eroded.territory_module");
            }

            @Override
            public ScreenHandler createMenu(
                    int syncId,
                    PlayerInventory playerInventory,
                    PlayerEntity player
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
            ServerWorld world,
            BlockPos anchorPos,
            ServerPlayerEntity manager,
            ServerPlayerEntity target,
            boolean connectedArea
    ) {
        TerritoryClaim claim = getAnchorClaim(world, anchorPos);

        if (claim == null) {
            claim = getActiveClaimAt(world, anchorPos);
        }

        if (claim == null) {
            manager.sendMessage(
                    Text.translatable("eroded.territory.trust.no_claim"),
                    true
            );
            return false;
        }

        if (!canManageClaim(manager, claim)) {
            manager.sendMessage(
                    Text.translatable("eroded.territory.trust.no_permission"),
                    true
            );
            return false;
        }

        if (claim.isOwner(target)) {
            manager.sendMessage(
                    Text.translatable("eroded.territory.trust.owner"),
                    true
            );
            return false;
        }

        List<TerritoryClaim> targetClaims = connectedArea
                ? findConnectedClaims(world, claim)
                : List.of(claim);

        boolean changed = false;

        for (TerritoryClaim targetClaim : targetClaims) {
            if (targetClaim.addTrusted(
                    target.getUuid(),
                    target.getName().getString()
            )) {
                changed = true;
            }
        }

        if (!changed) {
            manager.sendMessage(
                    Text.translatable(
                            "eroded.territory.trust.already",
                            target.getName().getString()
                    ),
                    true
            );
            return false;
        }

        TerritoryClaimState.get(world).markDirty();

        manager.sendMessage(
                Text.translatable(
                        connectedArea
                                ? "eroded.territory.trust.added.connected"
                                : "eroded.territory.trust.added",
                        target.getName().getString()
                ),
                true
        );

        target.sendMessage(
                Text.translatable(
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
            ServerWorld world,
            BlockPos anchorPos,
            ServerPlayerEntity manager,
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
            manager.sendMessage(
                    Text.translatable("eroded.territory.trust.no_claim"),
                    true
            );
            return false;
        }

        if (!canManageClaim(manager, claim)) {
            manager.sendMessage(
                    Text.translatable("eroded.territory.trust.no_permission"),
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

        for (TerritoryClaim targetClaim : targetClaims) {
            if (targetClaim.setTrustedPermission(
                    targetUuid,
                    permission,
                    enabled
            )) {
                changed = true;
            }
        }

        if (changed) {
            TerritoryClaimState.get(world).markDirty();
        }

        return changed;
    }

    public static boolean setTrustedScopeMode(
            ServerWorld world,
            BlockPos anchorPos,
            ServerPlayerEntity manager,
            UUID targetUuid,
            boolean connectedScopeMode
    ) {
        TerritoryClaim claim = getAnchorClaim(world, anchorPos);

        if (claim == null) {
            claim = getActiveClaimAt(world, anchorPos);
        }

        if (claim == null) {
            manager.sendMessage(
                    Text.translatable("eroded.territory.trust.no_claim"),
                    true
            );
            return false;
        }

        if (!canManageClaim(manager, claim)) {
            manager.sendMessage(
                    Text.translatable("eroded.territory.trust.no_permission"),
                    true
            );
            return false;
        }

        if (targetUuid == null) {
            return false;
        }

        if (claim.ownerUuid().equals(targetUuid)) {
            return false;
        }

        boolean changed = claim.setTrustedScopeMode(targetUuid, connectedScopeMode);

        if (changed) {
            TerritoryClaimState.get(world).markDirty();
        }

        return changed;
    }

    public static boolean addTrustedPlayer(
            ServerWorld world,
            BlockPos anchorPos,
            ServerPlayerEntity manager,
            ServerPlayerEntity target
    ) {
        return addTrustedPlayer(world, anchorPos, manager, target, false);
    }

    public static boolean removeTrustedPlayer(
            ServerWorld world,
            BlockPos anchorPos,
            ServerPlayerEntity manager,
            UUID targetUuid,
            String targetName,
            boolean connectedArea
    ) {
        TerritoryClaim claim = getAnchorClaim(world, anchorPos);

        if (claim == null) {
            claim = getActiveClaimAt(world, anchorPos);
        }

        if (claim == null) {
            manager.sendMessage(
                    Text.translatable("eroded.territory.trust.no_claim"),
                    true
            );
            return false;
        }

        if (!canManageClaim(manager, claim)) {
            manager.sendMessage(
                    Text.translatable("eroded.territory.trust.no_permission"),
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
            manager.sendMessage(
                    Text.translatable(
                            "eroded.territory.trust.not_trusted",
                            targetName
                    ),
                    true
            );
            return false;
        }

        TerritoryClaimState.get(world).markDirty();

        manager.sendMessage(
                Text.translatable(
                        connectedArea
                                ? "eroded.territory.trust.removed.connected"
                                : "eroded.territory.trust.removed",
                        targetName
                ),
                true
        );

        ServerPlayerEntity target = world.getServer()
                .getPlayerManager()
                .getPlayer(targetUuid);

        if (target != null) {
            target.sendMessage(
                    Text.translatable(
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
            ServerWorld world,
            BlockPos anchorPos,
            ServerPlayerEntity manager,
            UUID targetUuid,
            String targetName
    ) {
        return removeTrustedPlayer(world, anchorPos, manager, targetUuid, targetName, false);
    }

    public static List<ServerPlayerEntity> getOnlinePlayerSuggestions(
            ServerWorld world,
            TerritoryClaim claim,
            String query,
            int maxSuggestions
    ) {
        List<ServerPlayerEntity> suggestions = new ArrayList<>();

        if (claim == null || maxSuggestions <= 0) {
            return suggestions;
        }

        String normalizedQuery = query == null
                ? ""
                : query.trim().toLowerCase(Locale.ROOT);

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
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

    public static void refreshTrustedNamesFromOnlinePlayers(ServerWorld world, TerritoryClaim claim) {
        if (claim == null) {
            return;
        }

        boolean changed = false;

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (claim.isTrusted(player)) {
                if (claim.updateTrustedName(
                        player.getUuid(),
                        player.getName().getString()
                )) {
                    changed = true;
                }
            }
        }

        if (changed) {
            TerritoryClaimState.get(world).markDirty();
        }
    }

    public static void playAnchorActivatedSound(ServerWorld world, BlockPos pos) {
        world.playSound(
                null,
                pos,
                SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.BLOCKS,
                0.8F,
                1.15F
        );

        world.playSound(
                null,
                pos,
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.BLOCKS,
                0.7F,
                1.35F
        );
    }

    public static void playModuleInsertedSound(ServerWorld world, BlockPos pos) {
        world.playSound(
                null,
                pos,
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                SoundCategory.BLOCKS,
                0.8F,
                1.15F
        );

        world.playSound(
                null,
                pos,
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.BLOCKS,
                0.45F,
                1.65F
        );
    }

    private static void playTrustAddedSound(ServerPlayerEntity player) {
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                0.55F,
                1.25F
        );
    }

    private static void playTrustRemovedSound(ServerPlayerEntity player) {
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_ANVIL_LAND,
                SoundCategory.PLAYERS,
                0.35F,
                1.65F
        );
    }

    private static void playActionDeniedSound(ServerPlayerEntity player) {
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                SoundCategory.PLAYERS,
                0.45F,
                0.7F
        );
    }

    private static void playTerritoryModuleOpenSound(ServerPlayerEntity player) {
        player.playSoundToPlayer(
                SoundEvents.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.PLAYERS,
                0.8F,
                1.25F
        );

        player.playSoundToPlayer(
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                0.55F,
                1.45F
        );
    }

    private static void playPlacementHintActivatedSound(ServerPlayerEntity player) {
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.PLAYERS,
                0.55F,
                1.35F
        );

        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                0.35F,
                1.75F
        );
    }

    public static void syncInventory(ServerPlayerEntity player) {
        player.getInventory().markDirty();
        player.currentScreenHandler.syncState();
    }

    private static TerritoryClaim findNearestActiveClaimOwnedBy(
            ServerWorld world,
            ServerPlayerEntity player
    ) {
        TerritoryClaimState state = TerritoryClaimState.get(world);

        TerritoryClaim nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (TerritoryClaim claim : state.all()) {
            if (!claim.active()) {
                continue;
            }

            if (!claim.ownerUuid().equals(player.getUuid())) {
                continue;
            }

            if (!world.getBlockState(claim.anchorPos()).isOf(ErodedBlocks.TERRITORY_ANCHOR)) {
                state.remove(claim.anchorPos());
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
            ServerWorld world,
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
            ServerWorld world,
            TerritoryClaim startClaim
    ) {
        List<TerritoryClaim> result = new ArrayList<>();

        if (startClaim == null || !startClaim.active()) {
            return result;
        }

        TerritoryClaimState state = TerritoryClaimState.get(world);

        HashSet<BlockPos> visited = new HashSet<>();
        Queue<TerritoryClaim> queue = new ArrayDeque<>();

        queue.add(startClaim);
        visited.add(startClaim.anchorPos());

        while (!queue.isEmpty()) {
            TerritoryClaim current = queue.poll();

            if (!isClaimStillValid(world, current)) {
                state.remove(current.anchorPos());
                continue;
            }

            result.add(current);

            for (TerritoryClaim candidate : state.all()) {
                if (candidate == current) {
                    continue;
                }

                if (!candidate.active()) {
                    continue;
                }

                if (!candidate.ownerUuid().equals(startClaim.ownerUuid())) {
                    continue;
                }

                if (visited.contains(candidate.anchorPos())) {
                    continue;
                }

                if (!isClaimStillValid(world, candidate)) {
                    state.remove(candidate.anchorPos());
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

    private static boolean isClaimStillValid(ServerWorld world, TerritoryClaim claim) {
        return world.getBlockState(claim.anchorPos()).isOf(ErodedBlocks.TERRITORY_ANCHOR);
    }

    private static boolean areClaimsConnectedBySide(TerritoryClaim a, TerritoryClaim b) {
        boolean touchesOnX =
                claimMaxX(a) + 1 == claimMinX(b)
                        || claimMaxX(b) + 1 == claimMinX(a);

        boolean overlapsOnZ =
                rangesOverlap(
                        claimMinZ(a),
                        claimMaxZ(a),
                        claimMinZ(b),
                        claimMaxZ(b)
                );

        if (touchesOnX && overlapsOnZ) {
            return true;
        }

        boolean touchesOnZ =
                claimMaxZ(a) + 1 == claimMinZ(b)
                        || claimMaxZ(b) + 1 == claimMinZ(a);

        boolean overlapsOnX =
                rangesOverlap(
                        claimMinX(a),
                        claimMaxX(a),
                        claimMinX(b),
                        claimMaxX(b)
                );

        return touchesOnZ && overlapsOnX;
    }

    private static boolean rangesOverlap(int aMin, int aMax, int bMin, int bMax) {
        return aMin <= bMax && bMin <= aMax;
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
            ServerWorld world,
            ServerPlayerEntity player,
            boolean showMessage
    ) {
        TerritoryClaim claim = findNearestActiveClaimOwnedBy(world, player);

        if (claim == null) {
            if (showMessage) {
                player.sendMessage(
                        Text.translatable("eroded.territory.hint.no_active_claim"),
                        true
                );
            }

            return;
        }

        int spacing = claim.radius() + getConfiguredRadius() + 1;
        BlockPos anchor = claim.anchorPos();

        BlockPos[] suggestedPositions = new BlockPos[]{
                anchor.add(spacing, 0, 0),
                anchor.add(-spacing, 0, 0),
                anchor.add(0, 0, spacing),
                anchor.add(0, 0, -spacing)
        };

        for (BlockPos suggestedPos : suggestedPositions) {
            spawnSuggestionMarker(world, player, suggestedPos);
        }

        if (showMessage) {
            playPlacementHintActivatedSound(player);

            player.sendMessage(
                    Text.translatable(
                            "eroded.territory.hint.shown",
                            spacing
                    ),
                    true
            );
        }
    }

    private static void spawnSuggestionMarker(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos
    ) {
        int surfaceY = world.getTopY(
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(),
                pos.getZ()
        );

        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;

        for (int i = 0; i < 8; i++) {
            double y = surfaceY + 0.15 + (i * 0.3);

            world.spawnParticles(
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

        world.spawnParticles(
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
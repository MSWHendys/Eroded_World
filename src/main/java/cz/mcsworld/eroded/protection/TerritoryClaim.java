package cz.mcsworld.eroded.protection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

public final class TerritoryClaim {

    private static final int DEFAULT_RADIUS = 10;
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 256;
    public static final int MAX_TRUSTED_PLAYERS = 256;

    private final BlockPos anchorPos;
    private final UUID ownerUuid;
    private final String ownerName;

    private final Map<UUID, TrustedAccess> trustedAccess = new LinkedHashMap<>();

    private int radius;
    private boolean active;

    public TerritoryClaim(BlockPos anchorPos, UUID ownerUuid, String ownerName, int radius, boolean active) {
        this.anchorPos = anchorPos.immutable();
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.radius = clampRadius(radius);
        this.active = active;
    }

    public BlockPos anchorPos() {
        return anchorPos;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public String ownerName() {
        return ownerName;
    }

    public int radius() {
        return radius;
    }

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAnchor(BlockPos pos) {
        return anchorPos.equals(pos);
    }

    public boolean contains(BlockPos pos) {
        return Math.abs(pos.getX() - anchorPos.getX()) <= radius
                && Math.abs(pos.getZ() - anchorPos.getZ()) <= radius;
    }

    public boolean overlaps(BlockPos otherAnchorPos, int otherRadius) {
        int dx = Math.abs(otherAnchorPos.getX() - anchorPos.getX());
        int dz = Math.abs(otherAnchorPos.getZ() - anchorPos.getZ());

        return dx <= radius + otherRadius
                && dz <= radius + otherRadius;
    }

    public boolean isOwner(ServerPlayer player) {
        return ownerUuid.equals(player.getUUID());
    }

    public boolean isTrusted(ServerPlayer player) {
        return trustedAccess.containsKey(player.getUUID());
    }

    public boolean hasPermission(ServerPlayer player, TerritoryPermission permission) {
        if (isAdmin(player) || isOwner(player)) {
            return true;
        }

        TrustedAccess access = trustedAccess.get(player.getUUID());

        return access != null && access.has(permission);
    }

    private boolean isAdmin(ServerPlayer player) {
        return player.isCreative();
    }

    public Set<TrustedPlayer> trustedPlayerEntries() {
        Set<TrustedPlayer> result = new LinkedHashSet<>();

        for (TrustedAccess access : trustedAccess.values()) {
            result.add(
                    new TrustedPlayer(
                            access.uuid(),
                            access.name(),
                            access.flags(),
                            access.connectedScopeMode()
                    )
            );
        }

        return Collections.unmodifiableSet(result);
    }

    public boolean canAddTrusted(UUID uuid) {
        if (uuid == null || ownerUuid.equals(uuid)) {
            return false;
        }
        return trustedAccess.containsKey(uuid)
                || trustedAccess.size() < MAX_TRUSTED_PLAYERS;
    }

    public boolean addTrusted(UUID uuid, String name) {
        if (uuid == null) {
            return false;
        }

        if (ownerUuid.equals(uuid)) {
            return false;
        }

        TrustedAccess existing = trustedAccess.get(uuid);

        if (existing != null) {
            existing.updateName(name);
            return false;
        }

        if (trustedAccess.size() >= MAX_TRUSTED_PLAYERS) {
            return false;
        }

        TrustedAccess newAccess = TrustedAccess.fullAccess(uuid, name);

        Map<UUID, TrustedAccess> reordered = new LinkedHashMap<>();
        reordered.put(uuid, newAccess);
        reordered.putAll(trustedAccess);

        trustedAccess.clear();
        trustedAccess.putAll(reordered);

        return true;
    }

    public boolean removeTrusted(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        return trustedAccess.remove(uuid) != null;
    }

    public boolean updateTrustedName(UUID uuid, String name) {
        if (uuid == null || name == null || name.isBlank()) {
            return false;
        }

        TrustedAccess access = trustedAccess.get(uuid);

        if (access == null) {
            return false;
        }

        if (name.equals(access.name())) {
            return false;
        }

        access.updateName(name);
        return true;
    }

    public boolean setTrustedPermission(
            UUID uuid,
            TerritoryPermission permission,
            boolean enabled
    ) {
        if (uuid == null || permission == null) {
            return false;
        }

        TrustedAccess access = trustedAccess.get(uuid);

        if (access == null) {
            return false;
        }

        boolean before = access.has(permission);
        access.set(permission, enabled);

        return before != enabled;
    }

    public TrustedPlayer getTrustedPlayer(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        TrustedAccess access = trustedAccess.get(uuid);

        if (access == null) {
            return null;
        }

        return new TrustedPlayer(
                access.uuid(),
                access.name(),
                access.flags(),
                access.connectedScopeMode()
        );
    }

    /**
     * Creates or synchronizes one trusted-player entry. Used when a player is
     * switched to connected-area scope so every connected anchor has the same
     * access record before area-wide permission mutations are applied.
     */
    public boolean syncTrustedAccess(
            UUID uuid,
            String name,
            int flags,
            boolean connectedScopeMode
    ) {
        if (uuid == null || ownerUuid.equals(uuid)) {
            return false;
        }

        TrustedAccess access = trustedAccess.get(uuid);

        if (access == null) {
            if (trustedAccess.size() >= MAX_TRUSTED_PLAYERS) {
                return false;
            }

            TrustedAccess newAccess = new TrustedAccess(
                    uuid,
                    name,
                    flags,
                    connectedScopeMode
            );

            Map<UUID, TrustedAccess> reordered = new LinkedHashMap<>();
            reordered.put(uuid, newAccess);
            reordered.putAll(trustedAccess);

            trustedAccess.clear();
            trustedAccess.putAll(reordered);
            return true;
        }

        String beforeName = access.name();
        int beforeFlags = access.flags();
        boolean beforeScope = access.connectedScopeMode();

        access.updateName(name);
        access.setFlags(flags);
        access.setConnectedScopeMode(connectedScopeMode);

        return !beforeName.equals(access.name())
                || beforeFlags != access.flags()
                || beforeScope != access.connectedScopeMode();
    }

    public boolean setTrustedScopeMode(UUID uuid, boolean connectedScopeMode) {
        if (uuid == null) {
            return false;
        }

        TrustedAccess access = trustedAccess.get(uuid);

        if (access == null) {
            return false;
        }

        if (access.connectedScopeMode() == connectedScopeMode) {
            return false;
        }

        access.setConnectedScopeMode(connectedScopeMode);
        return true;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("x", anchorPos.getX());
        nbt.putInt("y", anchorPos.getY());
        nbt.putInt("z", anchorPos.getZ());

        nbt.putString("ownerUuid", ownerUuid.toString());
        nbt.putString("ownerName", ownerName);

        nbt.putInt("radius", radius);
        nbt.putBoolean("active", active);

        ListTag trusted = new ListTag();

        for (TrustedAccess access : trustedAccess.values()) {
            trusted.add(access.toNbt());
        }

        nbt.put("trusted", trusted);

        return nbt;
    }

    public static TerritoryClaim fromNbt(CompoundTag nbt) {
        BlockPos pos = new BlockPos(
                nbt.getIntOr("x", 0),
                nbt.getIntOr("y", 0),
                nbt.getIntOr("z", 0)
        );

        UUID ownerUuid;

        try {
            ownerUuid = UUID.fromString(nbt.getStringOr("ownerUuid", ""));
        } catch (IllegalArgumentException ex) {
            ownerUuid = new UUID(0L, 0L);
        }

        String ownerName = nbt.getStringOr("ownerName", "Unknown");
        int radius = nbt.getIntOr("radius", DEFAULT_RADIUS);
        boolean active = nbt.getBooleanOr("active", false);

        TerritoryClaim claim = new TerritoryClaim(
                pos,
                ownerUuid,
                ownerName,
                radius,
                active
        );

        ListTag trusted = nbt.getListOrEmpty("trusted");

        for (Tag element : trusted) {
            if (!(element instanceof CompoundTag entry)) {
                continue;
            }

            TrustedAccess access = TrustedAccess.fromNbt(entry);

            if (access.uuid() == null) {
                continue;
            }

            if (ownerUuid.equals(access.uuid())) {
                continue;
            }

            claim.trustedAccess.put(access.uuid(), access);
        }

        return claim;
    }

    private static int clampRadius(int radius) {
        return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
    }

    public record TrustedPlayer(
            UUID uuid,
            String name,
            int flags,
            boolean connectedScopeMode
    ) {
    }
}
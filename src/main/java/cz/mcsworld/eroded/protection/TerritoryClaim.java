package cz.mcsworld.eroded.protection;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TerritoryClaim {

    private static final int DEFAULT_RADIUS = 10;
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 256;

    private final BlockPos anchorPos;
    private final UUID ownerUuid;
    private final String ownerName;

    private final Map<UUID, TrustedAccess> trustedAccess = new LinkedHashMap<>();

    private int radius;
    private boolean active;

    public TerritoryClaim(BlockPos anchorPos, UUID ownerUuid, String ownerName, int radius, boolean active) {
        this.anchorPos = anchorPos.toImmutable();
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

    public boolean isOwner(ServerPlayerEntity player) {
        return ownerUuid.equals(player.getUuid());
    }

    public boolean isTrusted(ServerPlayerEntity player) {
        return trustedAccess.containsKey(player.getUuid());
    }

    public boolean hasPermission(ServerPlayerEntity player, TerritoryPermission permission) {
        if (isAdmin(player) || isOwner(player)) {
            return true;
        }

        TrustedAccess access = trustedAccess.get(player.getUuid());

        return access != null && access.has(permission);
    }

    private boolean isAdmin(ServerPlayerEntity player) {
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

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putInt("x", anchorPos.getX());
        nbt.putInt("y", anchorPos.getY());
        nbt.putInt("z", anchorPos.getZ());

        nbt.putString("ownerUuid", ownerUuid.toString());
        nbt.putString("ownerName", ownerName);

        nbt.putInt("radius", radius);
        nbt.putBoolean("active", active);

        NbtList trusted = new NbtList();

        for (TrustedAccess access : trustedAccess.values()) {
            trusted.add(access.toNbt());
        }

        nbt.put("trusted", trusted);

        return nbt;
    }

    public static TerritoryClaim fromNbt(NbtCompound nbt) {
        BlockPos pos = new BlockPos(
                nbt.getInt("x", 0),
                nbt.getInt("y", 0),
                nbt.getInt("z", 0)
        );

        UUID ownerUuid;

        try {
            ownerUuid = UUID.fromString(nbt.getString("ownerUuid", ""));
        } catch (IllegalArgumentException ex) {
            ownerUuid = new UUID(0L, 0L);
        }

        String ownerName = nbt.getString("ownerName", "Unknown");
        int radius = nbt.getInt("radius", DEFAULT_RADIUS);
        boolean active = nbt.getBoolean("active", false);

        TerritoryClaim claim = new TerritoryClaim(
                pos,
                ownerUuid,
                ownerName,
                radius,
                active
        );

        NbtList trusted = nbt.getListOrEmpty("trusted");

        for (NbtElement element : trusted) {
            if (!(element instanceof NbtCompound entry)) {
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
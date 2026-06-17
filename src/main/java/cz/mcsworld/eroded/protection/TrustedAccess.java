package cz.mcsworld.eroded.protection;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public final class TrustedAccess {

    private final UUID uuid;
    private String name;
    private int flags;
    private boolean connectedScopeMode;

    public TrustedAccess(UUID uuid, String name, int flags) {
        this(uuid, name, flags, false);
    }

    public TrustedAccess(UUID uuid, String name, int flags, boolean connectedScopeMode) {
        this.uuid = uuid;
        this.name = name == null || name.isBlank() ? "Unknown" : name;
        this.flags = TerritoryPermission.sanitize(flags);
        this.connectedScopeMode = connectedScopeMode;
    }

    public static TrustedAccess fullAccess(UUID uuid, String name) {
        return new TrustedAccess(
                uuid,
                name,
                TerritoryPermission.allMask(),
                false
        );
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public int flags() {
        return flags;
    }

    public boolean connectedScopeMode() {
        return connectedScopeMode;
    }

    public void setConnectedScopeMode(boolean connectedScopeMode) {
        this.connectedScopeMode = connectedScopeMode;
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            return;
        }

        this.name = name;
    }

    public boolean has(TerritoryPermission permission) {
        return permission != null && permission.isIn(flags);
    }

    public void set(TerritoryPermission permission, boolean enabled) {
        this.flags = TerritoryPermission.withPermission(
                this.flags,
                permission,
                enabled
        );
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putString("uuid", uuid.toString());
        nbt.putString("name", name);
        nbt.putInt("flags", flags);
        nbt.putBoolean("connectedScopeMode", connectedScopeMode);

        return nbt;
    }

    public static TrustedAccess fromNbt(NbtCompound nbt) {
        UUID uuid;

        try {
            uuid = UUID.fromString(nbt.getString("uuid", ""));
        } catch (IllegalArgumentException ex) {
            uuid = new UUID(0L, 0L);
        }

        String name = nbt.getString("name", "Unknown");

        int flags = nbt.contains("flags")
                ? nbt.getInt("flags", TerritoryPermission.allMask())
                : TerritoryPermission.allMask();

        boolean connectedScopeMode = nbt.getBoolean("connectedScopeMode", false);

        return new TrustedAccess(uuid, name, flags, connectedScopeMode);
    }
}
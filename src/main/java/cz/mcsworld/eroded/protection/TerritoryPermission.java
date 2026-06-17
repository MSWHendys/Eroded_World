package cz.mcsworld.eroded.protection;

public enum TerritoryPermission {

    BUILD(1),
    BREAK(1 << 1),
    CONTAINERS(1 << 2),
    REDSTONE(1 << 3),
    FIRE(1 << 4),
    ENTITIES(1 << 5);

    private final int bit;

    TerritoryPermission(int bit) {
        this.bit = bit;
    }

    public int bit() {
        return bit;
    }

    public boolean isIn(int flags) {
        return (flags & bit) != 0;
    }

    public static int allMask() {
        int mask = 0;

        for (TerritoryPermission permission : values()) {
            mask |= permission.bit;
        }

        return mask;
    }

    public static int sanitize(int flags) {
        return flags & allMask();
    }

    public static int withPermission(
            int flags,
            TerritoryPermission permission,
            boolean enabled
    ) {
        if (enabled) {
            return sanitize(flags | permission.bit());
        }

        return sanitize(flags & ~permission.bit());
    }
}
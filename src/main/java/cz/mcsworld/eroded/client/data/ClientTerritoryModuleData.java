package cz.mcsworld.eroded.client.data;

import cz.mcsworld.eroded.network.TerritoryModuleSyncPayload;
import cz.mcsworld.eroded.protection.TerritoryPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;

public final class ClientTerritoryModuleData {

    private static BlockPos anchorPos;
    private static String ownerName = "";
    private static int radius = 0;
    private static boolean active = false;
    private static int version = 0;

    private static int connectedWidth = 0;
    private static int connectedDepth = 0;
    private static int connectedClaimCount = 0;

    private static final List<Entry> trusted = new ArrayList<>();
    private static final List<Entry> suggestions = new ArrayList<>();

    private ClientTerritoryModuleData() {
    }

    public static void update(TerritoryModuleSyncPayload payload) {
        anchorPos = payload.anchorPos();
        ownerName = payload.ownerName();
        radius = payload.radius();
        active = payload.isActive();

        connectedWidth = payload.connectedWidth();
        connectedDepth = payload.connectedDepth();
        connectedClaimCount = payload.connectedClaimCount();

        trusted.clear();
        trusted.addAll(parse(payload.trustedData(), true));

        suggestions.clear();
        suggestions.addAll(parse(payload.suggestionData(), false));

        version++;
    }

    public static int version() {
        return version;
    }

    public static boolean isFor(BlockPos pos) {
        return anchorPos != null && anchorPos.equals(pos);
    }

    public static String ownerName() {
        return ownerName;
    }

    public static int radius() {
        return radius;
    }

    public static boolean active() {
        return active;
    }

    public static int connectedWidth() {
        return connectedWidth;
    }

    public static int connectedDepth() {
        return connectedDepth;
    }

    public static int connectedClaimCount() {
        return connectedClaimCount;
    }

    public static List<Entry> trusted() {
        return List.copyOf(trusted);
    }

    public static List<Entry> suggestions() {
        return List.copyOf(suggestions);
    }

    public static Entry trustedByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        for (Entry entry : trusted) {
            if (entry.uuid().equals(uuid)) {
                return entry;
            }
        }

        return null;
    }

    private static List<Entry> parse(String raw, boolean readFlags) {
        List<Entry> result = new ArrayList<>();

        if (raw == null || raw.isBlank()) {
            return result;
        }

        String[] lines = raw.split("\n");

        for (String line : lines) {

            String[] split = line.split("\\|", 4);

            if (split.length < 2) {
                continue;
            }

            try {
                UUID uuid = UUID.fromString(split[0]);
                String name = split[1];

                int flags = readFlags ? TerritoryPermission.allMask() : 0;
                boolean connectedScopeMode = false;

                if (readFlags && split.length >= 3) {
                    try {
                        flags = TerritoryPermission.sanitize(
                                Integer.parseInt(split[2])
                        );
                    } catch (NumberFormatException ignored) {
                        flags = TerritoryPermission.allMask();
                    }
                }

                if (readFlags && split.length >= 4) {
                    connectedScopeMode = parseBoolean(split[3]);
                }

                result.add(new Entry(uuid, name, flags, connectedScopeMode));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return result;
    }

    private static boolean parseBoolean(String raw) {
        return "true".equalsIgnoreCase(raw)
                || "1".equals(raw)
                || "yes".equalsIgnoreCase(raw);
    }

    public record Entry(UUID uuid, String name, int flags, boolean connectedScopeMode) {

        public Entry(UUID uuid, String name) {
            this(uuid, name, 0, false);
        }

        public Entry(UUID uuid, String name, int flags) {
            this(uuid, name, flags, false);
        }

        public boolean hasPermission(TerritoryPermission permission) {
            return permission != null && permission.isIn(flags);
        }
    }
}
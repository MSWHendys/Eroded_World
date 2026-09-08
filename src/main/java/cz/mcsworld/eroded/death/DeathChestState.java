package cz.mcsworld.eroded.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

public final class DeathChestState extends SavedData {

    public static final int SIZE = 54;

    public record StoredStack(ItemStack stack) {
        public static final Codec<StoredStack> CODEC =
                ItemStack.CODEC.xmap(
                        StoredStack::new,
                        StoredStack::stack
                );
    }

    public static final class Entry {

        private final BlockPos pos;
        private final UUID owner;
        private final long protectUntilEpochMs;
        private final Map<Integer, StoredStack> items;
        private final UUID hologramId;

        public Entry(
                BlockPos pos,
                UUID owner,
                long protectUntilEpochMs,
                Map<Integer, StoredStack> items,
                UUID hologramId
        ) {
            this.pos = pos;
            this.owner = owner;
            this.protectUntilEpochMs = protectUntilEpochMs;
            this.items = items;
            this.hologramId = hologramId;
        }

        public BlockPos pos() { return pos; }
        public UUID owner() { return owner; }
        public long protectUntilEpochMs() { return protectUntilEpochMs; }
        public Map<Integer, StoredStack> items() { return items; }
        public UUID hologramId() { return hologramId; }

        public boolean isProtected(long nowEpochMs) {
            return nowEpochMs < protectUntilEpochMs;
        }
    }

    private static final Codec<Entry> ENTRY_CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    BlockPos.CODEC.fieldOf("pos")
                            .forGetter(Entry::pos),

                    UUIDUtil.AUTHLIB_CODEC.fieldOf("owner")
                            .forGetter(Entry::owner),

                    Codec.LONG.fieldOf("until")
                            .forGetter(Entry::protectUntilEpochMs),

                    Codec.unboundedMap(Codec.STRING, StoredStack.CODEC)
                            .optionalFieldOf("items", Map.of())
                            .xmap(
                                    map -> {
                                        Map<Integer, StoredStack> out = new HashMap<>();
                                        map.forEach((k, v) ->
                                                out.put(Integer.parseInt(k), v));
                                        return out;
                                    },
                                    map -> {
                                        Map<String, StoredStack> out = new HashMap<>();
                                        map.forEach((k, v) ->
                                                out.put(String.valueOf(k), v));
                                        return out;
                                    }
                            )
                            .forGetter(Entry::items),

                    UUIDUtil.AUTHLIB_CODEC.fieldOf("hologramId")
                            .forGetter(Entry::hologramId)

            ).apply(inst, Entry::new));

    public static final Codec<DeathChestState> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    ENTRY_CODEC.listOf()
                            .optionalFieldOf("entries", List.of())
                            .forGetter(s ->
                                    new ArrayList<>(s.entries.values()))
            ).apply(inst, list -> {
                DeathChestState s = new DeathChestState();
                for (Entry e : list) {
                    s.entries.put(e.pos().asLong(), e);
                }
                return s;
            }));

    public static final SavedDataType<@NotNull DeathChestState> TYPE =
            new SavedDataType<>(Identifier.fromNamespaceAndPath
                    ("eroded","eroded_death_chest_state"),
                    DeathChestState::new,
                    CODEC,
                    DataFixTypes.LEVEL
            );

    private final Map<Long, Entry> entries = new HashMap<>();

    /**
     * Runtime-only GUI locks. They are deliberately not serialized: after a
     * restart no player can still own an open menu. The random session token
     * prevents an old menu close callback from unlocking a newer session.
     */
    private final Map<Long, OpenSession> openSessions = new HashMap<>();

    private record OpenSession(UUID playerId, UUID token) {}

    private DeathChestState() {}

    public static DeathChestState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    public static DeathChestState getIfPresent(ServerLevel world) {
        return world.getDataStorage().get(TYPE);
    }

    public Entry get(BlockPos pos) {
        return entries.get(pos.asLong());
    }

    public Collection<Entry> all() {
        return List.copyOf(entries.values());
    }

    public void put(
            BlockPos pos,
            UUID owner,
            long protectUntilEpochMs,
            Map<Integer, StoredStack> items,
            UUID hologramId
    ) {
        entries.put(
                pos.asLong(),
                new Entry(
                        pos.immutable(),
                        owner,
                        protectUntilEpochMs,
                        items,
                        hologramId
                )
        );
        setDirty();
    }

    public void remove(BlockPos pos) {
        long key = pos.asLong();
        openSessions.remove(key);
        if (entries.remove(key) != null) {
            setDirty();
        }
    }

    public UUID tryOpen(BlockPos pos, UUID playerId) {
        long key = pos.asLong();
        if (!entries.containsKey(key) || openSessions.containsKey(key)) {
            return null;
        }

        UUID token = UUID.randomUUID();
        openSessions.put(key, new OpenSession(playerId, token));
        return token;
    }

    public boolean isOpen(BlockPos pos) {
        return openSessions.containsKey(pos.asLong());
    }

    public boolean isOpenBy(BlockPos pos, UUID playerId, UUID token) {
        OpenSession session = openSessions.get(pos.asLong());
        return session != null
                && session.playerId().equals(playerId)
                && session.token().equals(token);
    }

    public void releaseOpen(BlockPos pos, UUID token) {
        long key = pos.asLong();
        OpenSession session = openSessions.get(key);
        if (session != null && session.token().equals(token)) {
            openSessions.remove(key);
        }
    }

    /**
     * Keeps persistent SavedData synchronized with an open GUI. This makes the
     * state authoritative during the session instead of leaving a full stale
     * copy in SavedData until the screen closes.
     */
    public boolean updateItems(
            BlockPos pos,
            UUID playerId,
            UUID token,
            List<ItemStack> inventory
    ) {
        if (!isOpenBy(pos, playerId, token)) {
            return false;
        }

        Entry old = get(pos);
        if (old == null) {
            return false;
        }

        entries.put(
                pos.asLong(),
                new Entry(
                        old.pos(),
                        old.owner(),
                        old.protectUntilEpochMs(),
                        fromInventory(inventory),
                        old.hologramId()
                )
        );
        setDirty();
        return true;
    }

    public boolean isProtected(BlockPos pos) {
        Entry e = get(pos);
        return e != null && e.isProtected(System.currentTimeMillis());
    }

    public static Map<Integer, StoredStack> fromInventory(List<ItemStack> list) {
        Map<Integer, StoredStack> map = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = list.get(i);
            if (!s.isEmpty()) {
                map.put(i, new StoredStack(s.copy()));
            }
        }
        return map;
    }

    public static List<ItemStack> toInventory(
            Map<Integer, StoredStack> map
    ) {
        List<ItemStack> list = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) list.add(ItemStack.EMPTY);

        map.forEach((slot, stored) -> {
            if (stored != null && !stored.stack().isEmpty()) {
                list.set(slot, stored.stack().copy());
            }
        });

        return list;
    }
}

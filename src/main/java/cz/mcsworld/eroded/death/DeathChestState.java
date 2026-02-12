package cz.mcsworld.eroded.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.*;

public final class DeathChestState extends PersistentState {

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

                    Uuids.CODEC.fieldOf("owner")
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

                    Uuids.CODEC.fieldOf("hologramId")
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

    public static final PersistentStateType<DeathChestState> TYPE =
            new PersistentStateType<>(
                    "eroded_death_chest_state",
                    ctx -> new DeathChestState(),
                    ctx -> CODEC,
                    DataFixTypes.LEVEL
            );

    private final Map<Long, Entry> entries = new HashMap<>();

    private DeathChestState() {}

    public static DeathChestState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public Entry get(BlockPos pos) {
        return entries.get(pos.asLong());
    }

    public Collection<Entry> all() {
        return entries.values();
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
                        pos.toImmutable(),
                        owner,
                        protectUntilEpochMs,
                        items,
                        hologramId
                )
        );
        markDirty();
    }

    public void remove(BlockPos pos) {
        if (entries.remove(pos.asLong()) != null) {
            markDirty();
        }
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

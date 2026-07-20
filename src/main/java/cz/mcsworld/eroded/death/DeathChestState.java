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

    private DeathChestState() {}

    public static DeathChestState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
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
        if (entries.remove(pos.asLong()) != null) {
            setDirty();
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

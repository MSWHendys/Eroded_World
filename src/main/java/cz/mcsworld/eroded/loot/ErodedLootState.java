package cz.mcsworld.eroded.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.mcsworld.eroded.ErodedMod;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class ErodedLootState extends SavedData {

    private final Map<Long, Set<UUID>> openedByPlayers;
    private final Set<Long> playerPlacedContainers;
    private final Set<Long> adminPlacedContainers;
    private final Set<Long> erodedGeneratedContainers;

    public ErodedLootState() {
        this(new HashMap<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    public ErodedLootState(Map<Long, Set<UUID>> openedByPlayers, Set<Long> playerPlacedContainers, Set<Long> adminPlacedContainers, Set<Long> erodedGeneratedContainers) {
        this.openedByPlayers = openedByPlayers;
        this.playerPlacedContainers = playerPlacedContainers;
        this.adminPlacedContainers = adminPlacedContainers;
        this.erodedGeneratedContainers = erodedGeneratedContainers;
    }

    private static final Codec<ErodedLootState> CODEC = RecordCodecBuilder.create(instance -> instance.group(

            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
                    .optionalFieldOf("openedByPlayers", new HashMap<>())
                    .forGetter(state -> {
                        Map<String, List<String>> out = new HashMap<>();
                        state.openedByPlayers.forEach((pos, players) ->
                                out.put(Long.toString(pos), players.stream().map(UUID::toString).toList()));
                        return out;
                    }),

            Codec.LONG.listOf()
                    .optionalFieldOf("playerPlacedContainers", new ArrayList<>())
                    .forGetter(state -> new ArrayList<>(state.playerPlacedContainers)),

            Codec.LONG.listOf()
                    .optionalFieldOf("adminPlacedContainers", new ArrayList<>())
                    .forGetter(state -> new ArrayList<>(state.adminPlacedContainers)),

            Codec.LONG.listOf()
                    .optionalFieldOf("erodedGeneratedContainers", new ArrayList<>())
                    .forGetter(state -> new ArrayList<>(state.erodedGeneratedContainers))

    ).apply(instance, (openedMapRaw, placedList, adminList, erodedList) -> {
        Map<Long, Set<UUID>> opened = new HashMap<>();
        openedMapRaw.forEach((posString, uuidList) -> {
            try {
                long pos = Long.parseLong(posString);
                Set<UUID> players = new HashSet<>();
                for (String s : uuidList) players.add(UUID.fromString(s));
                opened.put(pos, players);
            } catch (Exception ignored) {}
        });

        return new ErodedLootState(
                opened,
                new HashSet<>(placedList),
                new HashSet<>(adminList),
                new HashSet<>(erodedList)
        );
    }));

    public static final SavedDataType<ErodedLootState> TYPE = new SavedDataType<>(
            ErodedMod.MOD_ID + "_loot_state",
            ErodedLootState::new,
            CODEC,
            null
    );

    public static ErodedLootState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    public static ErodedLootState getIfPresent(ServerLevel world) {
        return world.getDataStorage().get(TYPE);
    }

    public boolean hasOpened(long pos, UUID player) {
        Set<UUID> set = openedByPlayers.get(pos);
        return set != null && set.contains(player);
    }

    public void markOpened(long pos, UUID player) {
        if (openedByPlayers.computeIfAbsent(pos, k -> new HashSet<>()).add(player)) setDirty();
    }

    public boolean hasAnyPlayerOpened(long pos) {
        Set<UUID> players = openedByPlayers.get(pos);
        return players != null && !players.isEmpty();
    }

    public void compactOpenedHistory(long pos) {
        Set<UUID> players = openedByPlayers.get(pos);
        if (players == null || players.size() <= 1) return;

        UUID keep = players.iterator().next();
        openedByPlayers.put(pos, new HashSet<>(Set.of(keep)));
        setDirty();
    }

    public void clearOpenedHistory(long pos) {
        if (openedByPlayers.remove(pos) != null) setDirty();
    }

    public boolean isPlayerPlaced(long pos) {
        return playerPlacedContainers.contains(pos);
    }

    public void markPlayerPlaced(long pos) {
        if (playerPlacedContainers.add(pos)) setDirty();
    }

    public void unmarkPlayerPlaced(long pos) {
        if (playerPlacedContainers.remove(pos)) setDirty();
    }

    public boolean isAdminPlaced(long pos) {
        return adminPlacedContainers.contains(pos);
    }

    public void markAdminPlaced(long pos) {
        if (adminPlacedContainers.add(pos)) setDirty();
    }

    public void unmarkAdminPlaced(long pos) {
        if (adminPlacedContainers.remove(pos)) setDirty();
    }

    public boolean isErodedGenerated(long pos) {
        return erodedGeneratedContainers.contains(pos);
    }

    public void markErodedGenerated(long pos) {
        if (erodedGeneratedContainers.add(pos)) setDirty();
    }

    public void unmarkErodedGenerated(long pos) {
        if (erodedGeneratedContainers.remove(pos)) setDirty();
    }

    /** Clears every piece of loot metadata for a physical coordinate. */
    public void resetPosition(long pos) {
        boolean changed = false;
        changed |= openedByPlayers.remove(pos) != null;
        changed |= playerPlacedContainers.remove(pos);
        changed |= adminPlacedContainers.remove(pos);
        changed |= erodedGeneratedContainers.remove(pos);
        if (changed) setDirty();
    }

    /**
     * Merges legacy per-half double-chest metadata into one canonical key.
     * Admin status wins over player status; player placement wins over
     * generated-world status. Opened-player history is unioned.
     */
    public long normalize(ErodedContainerIdentity.Identity identity) {
        long canonical = identity.canonicalKey();
        long[] keys = identity.memberKeys();

        if (keys.length == 1 && keys[0] == canonical) return canonical;

        boolean aliasHasData = false;
        for (long key : keys) {
            if (key == canonical) continue;
            aliasHasData |= openedByPlayers.containsKey(key)
                    || playerPlacedContainers.contains(key)
                    || adminPlacedContainers.contains(key)
                    || erodedGeneratedContainers.contains(key);
        }
        if (!aliasHasData) return canonical;

        Set<UUID> opened = new HashSet<>();
        boolean playerPlaced = false;
        boolean adminPlaced = false;
        boolean generated = false;
        boolean hadAny = false;

        for (long key : keys) {
            Set<UUID> players = openedByPlayers.get(key);
            if (players != null) {
                opened.addAll(players);
                hadAny = true;
            }
            playerPlaced |= playerPlacedContainers.contains(key);
            adminPlaced |= adminPlacedContainers.contains(key);
            generated |= erodedGeneratedContainers.contains(key);
            hadAny |= playerPlacedContainers.contains(key)
                    || adminPlacedContainers.contains(key)
                    || erodedGeneratedContainers.contains(key);
        }

        if (!hadAny) return canonical;

        for (long key : keys) {
            openedByPlayers.remove(key);
            playerPlacedContainers.remove(key);
            adminPlacedContainers.remove(key);
            erodedGeneratedContainers.remove(key);
        }

        if (!opened.isEmpty()) openedByPlayers.put(canonical, opened);

        if (adminPlaced) {
            adminPlacedContainers.add(canonical);
        } else if (playerPlaced) {
            playerPlacedContainers.add(canonical);
        } else if (generated) {
            erodedGeneratedContainers.add(canonical);
        }

        setDirty();
        return canonical;
    }

    public record ContainerSnapshot(
            Set<UUID> openedBy,
            boolean playerPlaced,
            boolean adminPlaced,
            boolean erodedGenerated
    ) {
        public boolean isEmpty() {
            return openedBy.isEmpty() && !playerPlaced && !adminPlaced && !erodedGenerated;
        }
    }

    public ContainerSnapshot captureAndClear(long[] keys) {
        Set<UUID> opened = new HashSet<>();
        boolean playerPlaced = false;
        boolean adminPlaced = false;
        boolean generated = false;
        boolean changed = false;

        for (long key : keys) {
            Set<UUID> players = openedByPlayers.get(key);
            if (players != null) opened.addAll(players);
            playerPlaced |= playerPlacedContainers.contains(key);
            adminPlaced |= adminPlacedContainers.contains(key);
            generated |= erodedGeneratedContainers.contains(key);
        }

        for (long key : keys) {
            changed |= openedByPlayers.remove(key) != null;
            changed |= playerPlacedContainers.remove(key);
            changed |= adminPlacedContainers.remove(key);
            changed |= erodedGeneratedContainers.remove(key);
        }

        if (changed) setDirty();
        return new ContainerSnapshot(opened, playerPlaced, adminPlaced, generated);
    }

    public void applySnapshot(long key, ContainerSnapshot snapshot) {
        resetPosition(key);
        boolean changed = false;

        if (!snapshot.openedBy().isEmpty()) {
            openedByPlayers.put(key, new HashSet<>(snapshot.openedBy()));
            changed = true;
        }

        if (snapshot.adminPlaced()) {
            changed |= adminPlacedContainers.add(key);
        } else if (snapshot.playerPlaced()) {
            changed |= playerPlacedContainers.add(key);
        } else if (snapshot.erodedGenerated()) {
            changed |= erodedGeneratedContainers.add(key);
        }

        if (changed) setDirty();
    }

    public static boolean isProtected(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (!(blockState.getBlock() instanceof ChestBlock || blockState.getBlock() instanceof BarrelBlock)) {
            return false;
        }

        ErodedLootState lootState = ErodedLootState.get(world);
        long key = lootState.normalize(ErodedContainerIdentity.resolve(world, pos, blockState));

        if (lootState.isAdminPlaced(key)) return true;
        if (lootState.isErodedGenerated(key)) return true;
        if (!lootState.isPlayerPlaced(key)) return !lootState.hasAnyPlayerOpened(key);
        return false;
    }
}

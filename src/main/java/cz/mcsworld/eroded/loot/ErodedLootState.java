package cz.mcsworld.eroded.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.mcsworld.eroded.ErodedMod;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

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

    public static final SavedDataType<@NotNull ErodedLootState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ErodedMod.MOD_ID, "loot_state"),
            ErodedLootState::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    public static ErodedLootState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
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

    public void clearOpenedHistory(long pos) {
        if (openedByPlayers.remove(pos) != null) {
            setDirty();
        }
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


    public static boolean isProtected(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock)) {
            return false;
        }

        ErodedLootState lootState = ErodedLootState.get(world);
        long key = pos.asLong();

        if (lootState.isAdminPlaced(key)) return true;

        if (lootState.isErodedGenerated(key)) return true;

        if (!lootState.isPlayerPlaced(key)) {
            return !lootState.hasAnyPlayerOpened(key);
        }

        return false;
    }
}
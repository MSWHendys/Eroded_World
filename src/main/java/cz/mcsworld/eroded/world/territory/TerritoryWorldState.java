package cz.mcsworld.eroded.world.territory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class TerritoryWorldState extends SavedData {

    private static final String ID = "eroded_territory";
    private final Map<TerritoryCellKey, TerritoryCell> cells = new HashMap<>();

    private record CellEntry(
            int x,
            int z,
            int miningScore,
            int mining,
            int pollution,
            int forestation,
            long lastTick,
            long lastActivityTick,
            long lastMiningActivityTick,
            long lastCollapseTick
    ) {}

    private static final Codec<CellEntry> CELL_ENTRY_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("x").forGetter(CellEntry::x),
                    Codec.INT.fieldOf("z").forGetter(CellEntry::z),
                    Codec.INT.fieldOf("miningScore").forGetter(CellEntry::miningScore),
                    Codec.INT.fieldOf("mining").forGetter(CellEntry::mining),
                    Codec.INT.fieldOf("pollution").forGetter(CellEntry::pollution),
                    Codec.INT.fieldOf("forestation").forGetter(CellEntry::forestation),
                    Codec.LONG.fieldOf("lastTick").forGetter(CellEntry::lastTick),
                    Codec.LONG.optionalFieldOf("lastActivityTick", 0L).forGetter(CellEntry::lastActivityTick),
                    Codec.LONG.optionalFieldOf("lastMiningActivityTick", 0L).forGetter(CellEntry::lastMiningActivityTick),
                    Codec.LONG.optionalFieldOf("lastCollapseTick", 0L).forGetter(CellEntry::lastCollapseTick)
            ).apply(instance, CellEntry::new));

    private static final Codec<TerritoryWorldState> CODEC =
            Codec.list(CELL_ENTRY_CODEC).fieldOf("cells").codec().xmap(
                    entries -> {
                        TerritoryWorldState state = new TerritoryWorldState();
                        for (CellEntry e : entries) {
                            TerritoryCell cell = new TerritoryCell();
                            cell.setMiningScore(e.miningScore());
                            cell.setMining(e.mining());
                            cell.setPollution(e.pollution());
                            cell.setForestation(e.forestation());
                            cell.setLastTick(e.lastTick());
                            cell.setLastActivityTick(e.lastActivityTick());
                            cell.setLastMiningActivityTick(e.lastMiningActivityTick());
                            cell.setLastCollapseTick(e.lastCollapseTick());
                            state.cells.put(new TerritoryCellKey(e.x(), e.z()), cell);
                        }
                        return state;
                    },
                    state -> {
                        List<CellEntry> entries = new ArrayList<>(state.cells.size());
                        for (Map.Entry<TerritoryCellKey, TerritoryCell> entry : state.cells.entrySet()) {
                            TerritoryCellKey key = entry.getKey();
                            TerritoryCell cell = entry.getValue();
                            entries.add(new CellEntry(
                                    key.cellX(),
                                    key.cellZ(),
                                    cell.getMiningScore(),
                                    cell.getMiningRaw(),
                                    cell.getPollutionRaw(),
                                    cell.getForestationRaw(),
                                    cell.getLastTick(),
                                    cell.getLastActivityTick(),
                                    cell.getLastMiningActivityTick(),
                                    cell.getLastCollapseTick()
                            ));
                        }
                        return entries;
                    }
            );

    public static final SavedDataType<TerritoryWorldState> TYPE =
            new SavedDataType<>(ID, TerritoryWorldState::new, CODEC, null);

    public static TerritoryWorldState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Read-only state lookup; does not create a new empty SavedData instance. */
    public static TerritoryWorldState getIfPresent(ServerLevel world) {
        return world.getDataStorage().get(TYPE);
    }

    public synchronized TerritoryCell getOrCreateCell(TerritoryCellKey key) {
        return cells.computeIfAbsent(key, k -> new TerritoryCell());
    }

    /** Read-only lookup: unlike getOrCreateCell(), this never grows SavedData. */
    public synchronized TerritoryCell getCell(TerritoryCellKey key) {
        return cells.get(key);
    }

    /** Safe copy for client/debug read paths; never exposes a live mutable cell. */
    public synchronized TerritoryCell copyCellOrEmpty(TerritoryCellKey key) {
        TerritoryCell cell = cells.get(key);
        return cell == null ? new TerritoryCell() : cell.copy();
    }

    public synchronized boolean hasCells() {
        return !cells.isEmpty();
    }

    public synchronized int size() {
        return cells.size();
    }

    /** Never expose the backing HashMap. */
    public synchronized Map<TerritoryCellKey, TerritoryCell> getCells() {
        Map<TerritoryCellKey, TerritoryCell> snapshot = new HashMap<>(cells.size());
        for (Map.Entry<TerritoryCellKey, TerritoryCell> entry : cells.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().copy());
        }
        return Map.copyOf(snapshot);
    }

    /**
     * Remove cells that have seen no real player-driven territory activity for
     * the configured retention window. No chunks are loaded and no world blocks
     * are touched by this operation.
     */
    public synchronized int pruneInactive(long now, long retentionTicks) {
        if (cells.isEmpty() || now <= 0L || retentionTicks <= 0L) {
            return 0;
        }

        int removed = 0;
        boolean changed = false;
        Iterator<Map.Entry<TerritoryCellKey, TerritoryCell>> iterator = cells.entrySet().iterator();

        while (iterator.hasNext()) {
            TerritoryCell cell = iterator.next().getValue();

            // Persist elapsed numeric decay while maintenance is already
            // touching the state. Ordinary reads remain side-effect free.
            if (cell.normalizeTo(now)) {
                changed = true;
            }

            long lastActivity = cell.getLastActivityTick();
            if (lastActivity <= 0L) {
                // Migration from older saves: empty historical/debug-only cells
                // can disappear immediately. Meaningful old cells get one full
                // retention window before they become eligible for pruning.
                if (cell.isEmptyAt(now)) {
                    iterator.remove();
                    removed++;
                } else {
                    cell.setLastActivityTick(now);
                    changed = true;
                }
                continue;
            }

            if (now >= lastActivity && now - lastActivity >= retentionTicks) {
                iterator.remove();
                removed++;
            }
        }

        if (changed || removed > 0) {
            setDirty();
        }
        return removed;
    }
}

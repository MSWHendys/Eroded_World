package cz.mcsworld.eroded.world.territory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TerritoryWorldState extends PersistentState {

    private static final String ID = "eroded_territory";
    private final Map<TerritoryCellKey, TerritoryCell> cells = new HashMap<>();
    private record CellEntry(int x, int z, int mining) {}

    private static final Codec<CellEntry> CELL_ENTRY_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("x").forGetter(CellEntry::x),
                    Codec.INT.fieldOf("z").forGetter(CellEntry::z),
                    Codec.INT.fieldOf("mining").forGetter(CellEntry::mining)
            ).apply(instance, CellEntry::new));

    private static final Codec<TerritoryWorldState> CODEC =
            Codec.list(CELL_ENTRY_CODEC).fieldOf("cells").codec().xmap(
                    entries -> {
                        TerritoryWorldState state = new TerritoryWorldState();
                        for (CellEntry e : entries) {
                            TerritoryCell cell = new TerritoryCell();
                            cell.setMiningScore(e.mining());
                            state.cells.put(
                                    new TerritoryCellKey(e.x(), e.z()),
                                    cell
                            );
                        }
                        return state;
                    },

                    state -> {
                        List<CellEntry> entries = new ArrayList<>(state.cells.size());
                        for (Map.Entry<TerritoryCellKey, TerritoryCell> entry : state.cells.entrySet()) {
                            TerritoryCellKey key = entry.getKey();
                            TerritoryCell cell = entry.getValue();
                            entries.add(
                                    new CellEntry(
                                            key.cellX(),
                                            key.cellZ(),
                                            cell.getMiningScore()
                                    )
                            );
                        }
                        return entries;
                    }
            );

    public static final PersistentStateType<TerritoryWorldState> TYPE =
            new PersistentStateType<>(
                    ID,
                    TerritoryWorldState::new,
                    CODEC,
                    null
            );

    public static TerritoryWorldState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public TerritoryCell getOrCreateCell(TerritoryCellKey key) {
        return cells.computeIfAbsent(key, k -> new TerritoryCell());
    }

    public boolean hasCells() {
        return !cells.isEmpty();
    }

    public Map<TerritoryCellKey, TerritoryCell> getCells() {
        return cells;
    }
}

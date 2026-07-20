package cz.mcsworld.eroded.world.territory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

public final class TerritoryWorldState extends SavedData {

    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("eroded","eroded_territory");
    private final Map<TerritoryCellKey, TerritoryCell> cells = new HashMap<>();
    private record CellEntry(
            int x,
            int z,
            int miningScore,
            int mining,
            int pollution,
            int forestation,
            long lastTick
    ) {}

    private static final Codec<CellEntry> CELL_ENTRY_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("x").forGetter(CellEntry::x),
                    Codec.INT.fieldOf("z").forGetter(CellEntry::z),
                    Codec.INT.fieldOf("miningScore").forGetter(CellEntry::miningScore),
                    Codec.INT.fieldOf("mining").forGetter(CellEntry::mining),
                    Codec.INT.fieldOf("pollution").forGetter(CellEntry::pollution),
                    Codec.INT.fieldOf("forestation").forGetter(CellEntry::forestation),
                    Codec.LONG.fieldOf("lastTick").forGetter(CellEntry::lastTick)
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
                                            cell.getMiningScore(),
                                            cell.getMiningRaw(),
                                            cell.getPollutionRaw(),
                                            cell.getForestationRaw(),
                                            cell.getLastTick()
                                    )
                            );
                        }
                        return entries;
                    }
            );

    public static final SavedDataType<@NotNull TerritoryWorldState> TYPE =
            new SavedDataType<>(
                    ID,
                    TerritoryWorldState::new,
                    CODEC,
                    null
            );

    public static TerritoryWorldState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
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

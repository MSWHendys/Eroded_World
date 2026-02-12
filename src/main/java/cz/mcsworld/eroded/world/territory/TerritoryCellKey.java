package cz.mcsworld.eroded.world.territory;

public record TerritoryCellKey(int cellX, int cellZ) {

    public static final int CELL_SIZE = 3;

    public static TerritoryCellKey fromChunk(int chunkX, int chunkZ) {
        return new TerritoryCellKey(
                Math.floorDiv(chunkX, CELL_SIZE),
                Math.floorDiv(chunkZ, CELL_SIZE)
        );
    }
}

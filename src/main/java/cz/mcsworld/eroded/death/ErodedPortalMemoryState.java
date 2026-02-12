package cz.mcsworld.eroded.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ErodedPortalMemoryState extends PersistentState {

    public record PortalRecord(long overworldPortalPos, long netherPortalPos) {
        public static final Codec<PortalRecord> CODEC =
                RecordCodecBuilder.create(inst -> inst.group(
                        Codec.LONG.fieldOf("ow").forGetter(PortalRecord::overworldPortalPos),
                        Codec.LONG.fieldOf("net").forGetter(PortalRecord::netherPortalPos)
                ).apply(inst, PortalRecord::new));
    }

    public static final Codec<ErodedPortalMemoryState> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.unboundedMap(Uuids.CODEC, PortalRecord.CODEC)
                            .optionalFieldOf("data", Map.of())
                            .forGetter(s -> s.data)
            ).apply(inst, map -> {
                ErodedPortalMemoryState s = new ErodedPortalMemoryState();
                s.data.putAll(map);
                return s;
            }));

    public static final PersistentStateType<ErodedPortalMemoryState> TYPE =
            new PersistentStateType<>(
                    "eroded_portal_memory",
                    ctx -> new ErodedPortalMemoryState(),
                    ctx -> CODEC,
                    DataFixTypes.LEVEL
            );

    private final Map<UUID, PortalRecord> data = new HashMap<>();

    private ErodedPortalMemoryState() {}

    public static ErodedPortalMemoryState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public void setOverworldPortal(UUID playerId, BlockPos pos) {
        PortalRecord cur = data.getOrDefault(playerId, new PortalRecord(0L, 0L));
        data.put(playerId, new PortalRecord(pos.asLong(), cur.netherPortalPos()));
        markDirty();
    }

    public void setNetherPortal(UUID playerId, BlockPos pos) {
        PortalRecord cur = data.getOrDefault(playerId, new PortalRecord(0L, 0L));
        data.put(playerId, new PortalRecord(cur.overworldPortalPos(), pos.asLong()));
        markDirty();
    }

    public BlockPos getOverworldPortal(UUID playerId) {
        PortalRecord r = data.get(playerId);
        if (r == null || r.overworldPortalPos() == 0L) return null;
        return BlockPos.fromLong(r.overworldPortalPos());
    }

    public BlockPos getNetherPortal(UUID playerId) {
        PortalRecord r = data.get(playerId);
        if (r == null || r.netherPortalPos() == 0L) return null;
        return BlockPos.fromLong(r.netherPortalPos());
    }
}
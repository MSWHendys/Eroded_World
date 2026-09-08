package cz.mcsworld.eroded.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

public final class ErodedPortalMemoryState extends SavedData {

    public record PortalRecord(long overworldPortalPos, long netherPortalPos) {
        public static final Codec<PortalRecord> CODEC =
                RecordCodecBuilder.create(inst -> inst.group(
                        Codec.LONG.fieldOf("ow").forGetter(PortalRecord::overworldPortalPos),
                        Codec.LONG.fieldOf("net").forGetter(PortalRecord::netherPortalPos)
                ).apply(inst, PortalRecord::new));
    }

    public static final Codec<ErodedPortalMemoryState> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, PortalRecord.CODEC)
                            .optionalFieldOf("data", Map.of())
                            .forGetter(s -> s.data)
            ).apply(inst, map -> {
                ErodedPortalMemoryState s = new ErodedPortalMemoryState();
                s.data.putAll(map);
                return s;
            }));

    public static final SavedDataType<@NotNull ErodedPortalMemoryState> TYPE =
            new SavedDataType<>(Identifier.fromNamespaceAndPath(
                    "eroded","eroded_portal_memory"),
                    ErodedPortalMemoryState::new,
                    CODEC,
                    DataFixTypes.LEVEL
            );

    private final Map<UUID, PortalRecord> data = new HashMap<>();

    private ErodedPortalMemoryState() {}

    public static ErodedPortalMemoryState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    public void setOverworldPortal(UUID playerId, BlockPos pos) {
        PortalRecord cur = data.getOrDefault(playerId, new PortalRecord(0L, 0L));
        data.put(playerId, new PortalRecord(pos.asLong(), cur.netherPortalPos()));
        setDirty();
    }

    public void setNetherPortal(UUID playerId, BlockPos pos) {
        PortalRecord cur = data.getOrDefault(playerId, new PortalRecord(0L, 0L));
        data.put(playerId, new PortalRecord(cur.overworldPortalPos(), pos.asLong()));
        setDirty();
    }

    public BlockPos getOverworldPortal(UUID playerId) {
        PortalRecord r = data.get(playerId);
        if (r == null || r.overworldPortalPos() == 0L) return null;
        return BlockPos.of(r.overworldPortalPos());
    }

    public BlockPos getNetherPortal(UUID playerId) {
        PortalRecord r = data.get(playerId);
        if (r == null || r.netherPortalPos() == 0L) return null;
        return BlockPos.of(r.netherPortalPos());
    }
}
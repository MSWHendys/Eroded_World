package cz.mcsworld.eroded.protection;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

public final class TerritoryClaimState extends SavedData {

    private static final Identifier STATE_ID =
            Identifier.fromNamespaceAndPath("eroded", "territory_claims");

    private static final Codec<TerritoryClaimState> CODEC =
            CompoundTag.CODEC.xmap(TerritoryClaimState::fromNbt, TerritoryClaimState::toNbt);

    private static final SavedDataType<@NotNull TerritoryClaimState> TYPE =
            new SavedDataType<>(
                    STATE_ID,
                    TerritoryClaimState::new,
                    CODEC,
                    null
            );

    private final Map<String, TerritoryClaim> claims = new HashMap<>();

    public static TerritoryClaimState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Returns an immutable snapshot of the current claims.
     *
     * <p>Never expose {@link Map#values()} here. Several protection paths may
     * discover and remove stale claims while iterating. Returning the live
     * values view makes those perfectly valid cleanups throw
     * ConcurrentModificationException.</p>
     */
    public List<TerritoryClaim> all() {
        return List.copyOf(claims.values());
    }

    public TerritoryClaim getByAnchor(BlockPos pos) {
        return claims.get(key(pos));
    }

    public void put(TerritoryClaim claim) {
        claims.put(key(claim.anchorPos()), claim);
        setDirty();
    }

    public TerritoryClaim remove(BlockPos pos) {
        TerritoryClaim removed = claims.remove(key(pos));

        if (removed != null) {
            setDirty();
        }

        return removed;
    }

    public TerritoryClaim findActiveAt(BlockPos pos) {
        for (TerritoryClaim claim : claims.values()) {
            if (claim.active() && claim.contains(pos)) {
                return claim;
            }
        }

        return null;
    }

    private static String key(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();

        for (TerritoryClaim claim : claims.values()) {
            list.add(claim.toNbt());
        }

        nbt.put("claims", list);
        return nbt;
    }

    public static TerritoryClaimState fromNbt(CompoundTag nbt) {
        TerritoryClaimState state = new TerritoryClaimState();

        ListTag list = nbt.getListOrEmpty("claims");

        for (Tag element : list) {
            if (!(element instanceof CompoundTag claimNbt)) {
                continue;
            }

            TerritoryClaim claim = TerritoryClaim.fromNbt(claimNbt);
            state.claims.put(key(claim.anchorPos()), claim);
        }

        return state;
    }

    public TerritoryClaim findOverlappingClaim(BlockPos anchorPos, int radius) {
        for (TerritoryClaim claim : claims.values()) {
            if (claim.isAnchor(anchorPos)) {
                continue;
            }

            if (claim.overlaps(anchorPos, radius)) {
                return claim;
            }
        }

        return null;
    }

    public int countByOwner(UUID ownerUuid) {
        int count = 0;

        for (TerritoryClaim claim : claims.values()) {
            if (claim.ownerUuid().equals(ownerUuid)) {
                count++;
            }
        }

        return count;
    }


}
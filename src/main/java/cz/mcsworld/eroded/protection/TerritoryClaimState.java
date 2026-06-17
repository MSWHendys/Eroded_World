package cz.mcsworld.eroded.protection;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TerritoryClaimState extends PersistentState {

    private static final String STATE_ID = "eroded_territory_claims";

    private static final Codec<TerritoryClaimState> CODEC =
            NbtCompound.CODEC.xmap(TerritoryClaimState::fromNbt, TerritoryClaimState::toNbt);

    private static final PersistentStateType<TerritoryClaimState> TYPE =
            new PersistentStateType<>(
                    STATE_ID,
                    TerritoryClaimState::new,
                    CODEC,
                    null
            );

    private final Map<String, TerritoryClaim> claims = new HashMap<>();

    public static TerritoryClaimState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public Collection<TerritoryClaim> all() {
        return claims.values();
    }

    public TerritoryClaim getByAnchor(BlockPos pos) {
        return claims.get(key(pos));
    }

    public void put(TerritoryClaim claim) {
        claims.put(key(claim.anchorPos()), claim);
        markDirty();
    }

    public TerritoryClaim remove(BlockPos pos) {
        TerritoryClaim removed = claims.remove(key(pos));

        if (removed != null) {
            markDirty();
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

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtList list = new NbtList();

        for (TerritoryClaim claim : claims.values()) {
            list.add(claim.toNbt());
        }

        nbt.put("claims", list);
        return nbt;
    }

    public static TerritoryClaimState fromNbt(NbtCompound nbt) {
        TerritoryClaimState state = new TerritoryClaimState();

        NbtList list = nbt.getListOrEmpty("claims");

        for (NbtElement element : list) {
            if (!(element instanceof NbtCompound claimNbt)) {
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
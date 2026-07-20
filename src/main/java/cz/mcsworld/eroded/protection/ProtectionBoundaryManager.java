package cz.mcsworld.eroded.protection;

import cz.mcsworld.eroded.world.spawn.ExplosionProtectionManager;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ProtectionBoundaryManager {

    private ProtectionBoundaryManager() {
    }

    public static boolean canAutomationMoveBetween(
            ServerLevel world,
            BlockPos from,
            BlockPos to
    ) {
        return Objects.equals(
                getProtectionKey(world, from),
                getProtectionKey(world, to)
        );
    }



    public static boolean canPistonMoveBlock(
            ServerLevel world,
            BlockPos from,
            BlockPos to
    ) {
        return canAutomationMoveBetween(world, from, to);
    }

    public static boolean canPistonBreakBlock(ServerLevel world, BlockPos pos) {
        return !isAnyProtected(world, pos);
    }



    public static boolean isAnyProtected(ServerLevel world, BlockPos pos) {
        if (ExplosionProtectionManager.isProtected(world, pos)) {
            return true;
        }

        return TerritoryProtectionManager.getActiveClaimAt(world, pos) != null;
    }

    private static ProtectionKey getProtectionKey(ServerLevel world, BlockPos pos) {
        if (ExplosionProtectionManager.isProtected(world, pos)) {
            return ProtectionKey.spawn();
        }

        TerritoryClaim claim = TerritoryProtectionManager.getActiveClaimAt(world, pos);

        if (claim == null) {
            return ProtectionKey.none();
        }

        BlockPos groupRoot = getConnectedClaimRoot(world, claim);

        return ProtectionKey.claim(claim.ownerUuid(), groupRoot);
    }

    private static BlockPos getConnectedClaimRoot(ServerLevel world, TerritoryClaim claim) {
        List<TerritoryClaim> connectedClaims =
                TerritoryProtectionManager.findConnectedClaims(world, claim);

        return connectedClaims.stream()
                .map(TerritoryClaim::anchorPos)
                .min(Comparator
                        .comparingInt((BlockPos pos) -> pos.getX())
                        .thenComparingInt(pos -> pos.getY())
                        .thenComparingInt(pos -> pos.getZ()))
                .orElse(claim.anchorPos());
    }

    private record ProtectionKey(
            Type type,
            UUID ownerUuid,
            BlockPos rootPos
    ) {
        private static ProtectionKey none() {
            return new ProtectionKey(Type.NONE, null, null);
        }

        private static ProtectionKey spawn() {
            return new ProtectionKey(Type.SPAWN, null, BlockPos.ZERO);
        }

        private static ProtectionKey claim(UUID ownerUuid, BlockPos rootPos) {
            return new ProtectionKey(Type.CLAIM, ownerUuid, rootPos);
        }
    }

    private enum Type {
        NONE,
        SPAWN,
        CLAIM
    }
}
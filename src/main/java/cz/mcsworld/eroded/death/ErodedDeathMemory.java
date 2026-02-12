package cz.mcsworld.eroded.death;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.UUID;

public final class ErodedDeathMemory {

    private final BlockPos deathPos;
    private final RegistryKey<World> deathDimension;
    private long expireEpochMs;
    private final long value;
    private final UUID hologramId;
    private boolean resolved;

    public ErodedDeathMemory(
            BlockPos deathPos,
            RegistryKey<World> deathDimension,
            long expireEpochMs,
            long value,
            UUID hologramId
    ) {
        this.deathPos = deathPos;
        this.deathDimension = deathDimension;
        this.expireEpochMs = expireEpochMs;
        this.value = value;
        this.hologramId = hologramId;
    }


    public static long calculateDynamicExpireMs(BlockPos deathPos, BlockPos respawnPos) {
        double distance = Math.sqrt(deathPos.getSquaredDistance(respawnPos));

        double baseMin = 5.0;
        double bonusMin = (distance / 200.0) * 1.5;

        long totalMs = (long) ((baseMin + bonusMin) * 60.0 * 1000.0);

        totalMs = Math.max(300000L, Math.min(totalMs, 2700000L));

        return System.currentTimeMillis() + totalMs;
    }

    public void accelerateExpiraton(int seconds) {
        long newExpire = System.currentTimeMillis() + (seconds * 1000L);
        if (newExpire < this.expireEpochMs) {
            this.expireEpochMs = newExpire;
        }
    }

    public BlockPos getDeathPos() {
        return deathPos;
    }

    public RegistryKey<World> getDeathDimension() {
        return deathDimension;
    }

    public long getValue() {
        return value;
    }

    public UUID getHologramId() {
        return hologramId;
    }

    public boolean isExpired(long ignoredNowTick) {
        return System.currentTimeMillis() >= expireEpochMs;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void resolve() {
        this.resolved = true;
    }

    public long getRemainingTicks(long ignoredNowTick) {
        long remainingMs = expireEpochMs - System.currentTimeMillis();
        return Math.max(0, (remainingMs / 1000L) * 20L);
    }
}
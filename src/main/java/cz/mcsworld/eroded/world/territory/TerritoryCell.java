package cz.mcsworld.eroded.world.territory;

/**
 * Mutable territory state for one territory cell.
 *
 * Time values stored here use ServerLevel#getGameTime(). Unlike the server
 * process tick counter, game time survives a restart, so decay/retention keeps
 * working correctly across server restarts.
 */
public final class TerritoryCell {

    private long lastMiningActivityTick;
    private long lastCollapseTick;
    private long lastActivityTick;
    private int miningScore;

    private int mining;
    private int pollution;
    private int forestation;

    private long lastTick;

    public void setMiningScore(int value) {
        this.miningScore = Math.max(0, value);
    }

    public void setMining(int value) {
        this.mining = Math.max(0, value);
    }

    public void setPollution(int value) {
        this.pollution = Math.max(0, value);
    }

    public void setForestation(int value) {
        this.forestation = Math.max(0, value);
    }

    public void setLastTick(long value) {
        this.lastTick = Math.max(0L, value);
    }

    public void setLastActivityTick(long value) {
        this.lastActivityTick = Math.max(0L, value);
    }

    public long getLastActivityTick() {
        return lastActivityTick;
    }

    public int getMiningRaw() {
        return mining;
    }

    public int getPollutionRaw() {
        return pollution;
    }

    public int getForestationRaw() {
        return forestation;
    }

    public long getLastTick() {
        return lastTick;
    }

    public void incrementMiningScore() {
        miningScore++;
    }

    public int getMiningScore() {
        return miningScore;
    }

    /**
     * Read-only projected values. Reads no longer mutate SavedData as a side
     * effect; persistent normalization is handled by writes/maintenance.
     */
    public int getMining(long tick) {
        return decayedValue(mining, tick);
    }

    public int getPollution(long tick) {
        return decayedValue(pollution, tick);
    }

    public int getForestation(long tick) {
        return decayedValue(forestation, tick);
    }

    public void addMining(int value, long tick) {
        normalizeTo(tick);
        mining = Math.max(0, mining + value);
        lastTick = tick;
    }

    public void addPollution(int value, long tick) {
        normalizeTo(tick);
        pollution = Math.max(0, pollution + value);
        lastTick = tick;
    }

    public void addForestation(int value, long tick) {
        normalizeTo(tick);
        forestation = Math.max(0, forestation + value);
        lastTick = tick;
    }

    /** Marks real player-driven territory activity for retention/pruning. */
    public void touchActivity(long tick) {
        if (tick > 0L) {
            lastActivityTick = tick;
        }
    }

    public void setLastMiningActivityTick(long tick) {
        this.lastMiningActivityTick = Math.max(0L, tick);
    }

    public long getLastMiningActivityTick() {
        return lastMiningActivityTick;
    }

    /**
     * Tick of the last cave-collapse event (including a collapse prevented by
     * a stabilizer/warding lamp). This is deliberately separate from mining
     * activity so continuous mining does not permanently suppress collapse.
     */
    public void setLastCollapseTick(long tick) {
        this.lastCollapseTick = Math.max(0L, tick);
    }

    public long getLastCollapseTick() {
        return lastCollapseTick;
    }

    /**
     * Materialize elapsed decay into the persisted counters.
     *
     * @return true when persisted state changed.
     */
    public boolean normalizeTo(long tick) {
        if (tick <= 0L) {
            return false;
        }

        // Once all decayable counters are already zero there is nothing to
        // materialize. Avoid dirtying SavedData every maintenance interval.
        if (mining <= 0 && pollution <= 0 && forestation <= 0) {
            return false;
        }

        if (lastTick <= 0L) {
            lastTick = tick;
            return true;
        }

        // Defensive migration guard: old builds used the process tick counter.
        // If that value is ahead of the world's persistent game time, rebase it
        // instead of producing a negative elapsed duration.
        if (tick < lastTick) {
            lastTick = tick;
            return true;
        }

        long elapsed = tick - lastTick;
        if (elapsed < 1200L) {
            return false;
        }

        long rawSteps = elapsed / 1200L;
        int steps = rawSteps > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawSteps;

        int oldMining = mining;
        int oldPollution = pollution;
        int oldForestation = forestation;

        mining = Math.max(0, mining - steps);
        pollution = Math.max(0, pollution - steps);
        forestation = Math.max(0, forestation - steps);

        lastTick += rawSteps * 1200L;
        return oldMining != mining || oldPollution != pollution || oldForestation != forestation || rawSteps > 0L;
    }

    public boolean isEmptyAt(long tick) {
        return miningScore <= 0
                && getMining(tick) <= 0
                && getPollution(tick) <= 0
                && getForestation(tick) <= 0;
    }

    public TerritoryCell copy() {
        TerritoryCell copy = new TerritoryCell();
        copy.lastMiningActivityTick = lastMiningActivityTick;
        copy.lastCollapseTick = lastCollapseTick;
        copy.lastActivityTick = lastActivityTick;
        copy.miningScore = miningScore;
        copy.mining = mining;
        copy.pollution = pollution;
        copy.forestation = forestation;
        copy.lastTick = lastTick;
        return copy;
    }

    private int decayedValue(int value, long tick) {
        if (value <= 0 || lastTick <= 0L || tick <= lastTick) {
            return Math.max(0, value);
        }

        long steps = (tick - lastTick) / 1200L;
        if (steps <= 0L) {
            return value;
        }
        if (steps >= value) {
            return 0;
        }
        return value - (int) steps;
    }
}

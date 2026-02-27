package cz.mcsworld.eroded.world.territory;



public final class TerritoryCell {


    public void setMiningScore(int value) {
        this.miningScore = value;
    }

    private long lastMiningActivityTick;
    private int miningScore;

    // ECO
    private int mining;
    private int pollution;
    private int forestation;

    private long lastTick;

    public void setMining(int value) {
        this.mining = value;
    }

    public void setPollution(int value) {
        this.pollution = value;
    }

    public void setForestation(int value) {
        this.forestation = value;
    }

    public void setLastTick(long value) {
        this.lastTick = value;
    }

// ===== RAW GETTERY (pro NBT save) =====

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
    // ================= GEO =================

    public void incrementMiningScore() {
        miningScore++;
    }

    public int getMiningScore() {
        return miningScore;
    }

    // ================= ECO =================

    public int getMining(long tick) {
        applyDecay(tick);
        return mining;
    }

    public int getPollution(long tick) {
        applyDecay(tick);
        return pollution;
    }

    public int getForestation(long tick) {
        applyDecay(tick);
        return forestation;
    }

    public void addMining(int value, long tick) {
        applyDecay(tick);
        mining = Math.max(0, mining + value);
        lastTick = tick;
    }

    public void addPollution(int value, long tick) {
        applyDecay(tick);
        pollution = Math.max(0, pollution + value);
        lastTick = tick;
    }

    public void addForestation(int value, long tick) {
        applyDecay(tick);
        forestation = Math.max(0, forestation + value);
        lastTick = tick;
    }

    public void setLastMiningActivityTick(long tick) {
        this.lastMiningActivityTick = tick;
    }

    public long getLastMiningActivityTick() {
        return lastMiningActivityTick;
    }

    private void applyDecay(long tick) {
        if (lastTick == 0) {
            lastTick = tick;
            return;
        }

        long elapsed = tick - lastTick;
        if (elapsed < 1200) return;

        int steps = (int)(elapsed / 1200);

        mining = Math.max(0, mining - steps);
        pollution = Math.max(0, pollution - steps);
        forestation = Math.max(0, forestation - steps);

        lastTick += (long)steps * 1200;
    }
}


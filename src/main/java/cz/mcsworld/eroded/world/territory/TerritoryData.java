package cz.mcsworld.eroded.world.territory;

import net.minecraft.nbt.NbtCompound;

public class TerritoryData {

    private int mining;
    private int forestation;
    private int pollution;
    private long lastTick;

    public int getMining(long tick) {
        applyDecay(tick);
        return mining;
    }

    public int getForestation(long tick) {
        applyDecay(tick);
        return forestation;
    }

    public int getPollution(long tick) {
        applyDecay(tick);
        return pollution;
    }

    public void addMining(int value, long tick) {
        applyDecay(tick);
        mining = Math.max(0, mining + value);
        lastTick = tick;
    }

    public void addForestation(int value, long tick) {
        applyDecay(tick);
        forestation = Math.max(0, forestation + value);
        lastTick = tick;
    }

    public void addPollution(int value, long tick) {
        applyDecay(tick);
        pollution = Math.max(0, pollution + value);
        lastTick = tick;
    }

    private void applyDecay(long tick) {
        if (lastTick == 0) {
            lastTick = tick;
            return;
        }

        long elapsed = tick - lastTick;
        if (elapsed < 1200) return;

        int steps = (int) (elapsed / 1200);

        mining = Math.max(0, mining - steps);
        forestation = Math.max(0, forestation - steps);
        pollution = Math.max(0, pollution - steps);

        lastTick += (long) steps * 1200;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("mining", mining);
        nbt.putInt("forestation", forestation);
        nbt.putInt("pollution", pollution);
        nbt.putLong("lastTick", lastTick);
        return nbt;
    }

    public static TerritoryData fromNbt(NbtCompound nbt) {
        TerritoryData d = new TerritoryData();
        nbt.getInt("mining").ifPresent(v -> d.mining = v);
        nbt.getInt("forestation").ifPresent(v -> d.forestation = v);
        nbt.getInt("pollution").ifPresent(v -> d.pollution = v);
        nbt.getLong("lastTick").ifPresent(v -> d.lastTick = v);
        return d;
    }
}

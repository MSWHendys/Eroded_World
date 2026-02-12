package cz.mcsworld.eroded.world.territory;

import net.minecraft.nbt.NbtCompound;

public final class TerritoryCell {

    private int miningScore;

    public void incrementMining() {
        miningScore++;
    }

    public int getMiningScore() {
        return miningScore;
    }

    public void setMiningScore(int value) {
        this.miningScore = value;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("mining", miningScore);
        return nbt;
    }

    public static TerritoryCell fromNbt(NbtCompound nbt) {
        TerritoryCell cell = new TerritoryCell();
        cell.miningScore = nbt.getInt("mining").orElse(0);
        return cell;
    }
}

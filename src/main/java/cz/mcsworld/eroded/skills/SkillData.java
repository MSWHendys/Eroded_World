package cz.mcsworld.eroded.skills;

import cz.mcsworld.eroded.config.energy.EnergyConfig;

import java.util.EnumMap;
import java.util.Map;

public class SkillData {

    private final Map<SkillType, Float> cgMap = new EnumMap<>(SkillType.class);

    public SkillData() {
        for (SkillType type : SkillType.values()) {
            cgMap.put(type, 0.0f);
        }
    }

    public float getCg(SkillType type) {
        return cgMap.getOrDefault(type, 0.0f);
    }

    public void addCg(SkillType type, float baseAmount) {
        float current = getCg(type);
        if (current >= 100.0f) return;

        float multiplier = 1.0f - (current / 120.0f);
        float gained = baseAmount * Math.max(multiplier, 0.05f);

        cgMap.put(type, Math.min(current + gained, 100.0f));
    }

    public void setCg(SkillType type, float value) {
        cgMap.put(type, Math.max(0.0f, Math.min(100.0f, value)));
    }

    private int energy;
    private long lastRegenTime = System.currentTimeMillis();

    private boolean collapsed = false;
    private long collapseUntilMs = 0;

    public enum EnergyState {
        NORMAL,
        TIRED,
        EXHAUSTED,
        EMPTY
    }

    private EnergyState lastEnergyState = EnergyState.NORMAL;

    public static int severity(EnergyState state) {
        return switch (state) {
            case NORMAL -> 0;
            case TIRED -> 1;
            case EXHAUSTED -> 2;
            case EMPTY -> 3;
        };
    }

    private EnergyState calculateEnergyState() {
        var root = EnergyConfig.get();
        var cfg = root.server.thresholds;
        if (collapsed) {
            return EnergyState.EMPTY;
        }

        int max = getMaxEnergy();
        if (max <= 0) return EnergyState.NORMAL;

        float percent = (energy / (float) max) * 100f;

        if (percent <= cfg.emptyPercent) return EnergyState.EMPTY;
        if (percent <= cfg.exhaustedPercent) return EnergyState.EXHAUSTED;
        if (percent <= cfg.tiredPercent) return EnergyState.TIRED;
        return EnergyState.NORMAL;
    }

    public EnergyState detectWorseningState() {

        EnergyState current = calculateEnergyState();

        if (severity(current) > severity(lastEnergyState)) {
            lastEnergyState = current;
            return current;
        }

        if (severity(current) < severity(lastEnergyState)) {
            lastEnergyState = current;
        }

        return null;
    }

    public EnergyState getEnergyState() {
        return calculateEnergyState();
    }

    public int getEnergy() {
        regenerateEnergy();
        return energy;
    }

    public int getMaxEnergy() {
        return getConfig().server.core.maxEnergy;
    }

    public boolean hasEnoughEnergy(int amount) {
        regenerateEnergy();
        return !collapsed && energy >= amount;
    }

    public boolean canAffordEnergy(int amount) {
        regenerateEnergy();
        return !collapsed && energy >= amount;
    }

    public boolean tryConsumeEnergy(int amount) {
        if (!hasEnoughEnergy(amount)) return false;
        consumeEnergy(amount);
        return true;
    }

    public void consumeEnergy(int amount) {
        if (collapsed || amount <= 0) return;

        regenerateEnergy();

        energy = Math.max(0, energy - amount);

        if (energy == 0) {
            enterCollapse();
            return;
        }
    }

    public void addEnergy(int amount) {
        regenerateEnergy();
        if (amount <= 0) return;

        energy = Math.min(getMaxEnergy(), energy + amount);

        if (energy > 0) {
            collapsed = false;
        }

        lastEnergyState = calculateEnergyState();
    }

    public void setEnergy(int value) {
        energy = Math.max(0, Math.min(getMaxEnergy(), value));
        collapsed = false;

        lastEnergyState = calculateEnergyState();
    }

    public void setEnergyAfterDeath(float ratio) {
        int target = Math.max(0, (int) (getMaxEnergy() * ratio));
        setEnergy(target);
    }

    public void initialize() {
        energy = getMaxEnergy();
        collapsed = false;
        lastRegenTime = System.currentTimeMillis();

        lastEnergyState = calculateEnergyState();
    }

    private void enterCollapse() {
        var energyRoot = EnergyConfig.get();
        var cfg = energyRoot.server.collapse;
        collapsed = true;
        collapseUntilMs = System.currentTimeMillis() + cfg.collapseDelayMs;
    }

    private void regenerateEnergy() {
        var root = EnergyConfig.get();
        var cfg = root.server.regen;
        long now = System.currentTimeMillis();

        if (collapsed) {
            if (now < collapseUntilMs) {
                return;
            }
            energy = 1;
            collapsed = false;
            lastRegenTime = now;
            return;
        }

        if (!cfg.passiveRegenEnabled) return;

        long intervalMs = cfg.regenIntervalSeconds * 1000L;
        long elapsed = now - lastRegenTime;
        if (elapsed < intervalMs) return;

        int restoredSegments = (int) (elapsed / intervalMs);
        int restoreAmount = restoredSegments * root.server.core.energyPerSegment;

        if (restoreAmount > 0) {
            energy = Math.min(getMaxEnergy(), energy + restoreAmount);
            lastRegenTime += restoredSegments * intervalMs;
        }
    }


    private int lastFlashes = -1;
/**
    public boolean areFlashesDecreasing() {
        var root = EnergyConfig.get();
        var cfg = root;
        int flashes = getCurrentFlashes(root);

        if (lastFlashes == -1) {
            lastFlashes = flashes;
            return false;
        }

        boolean decreasing = flashes < lastFlashes;
        lastFlashes = flashes;
        return decreasing;
    }

    private int getCurrentFlashes(EnergyConfig cfg) {
        float energyPerFlash = (float) getMaxEnergy() / cfg.client.hud.numberEnergyFlashes;
        return (int) (energy / energyPerFlash);
    } */

    private EnergyConfig getConfig() {
        return EnergyConfig.get();
    }
}
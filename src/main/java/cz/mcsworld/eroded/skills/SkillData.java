package cz.mcsworld.eroded.skills;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class SkillData {


    private final Map<SkillType, Float> cgMap = new EnumMap<>(SkillType.class);

    public SkillData() {
        for (SkillType type : SkillType.values()) {
            cgMap.put(type, 0.0f);
        }
    }
    private static final Logger LOGGER =
            LoggerFactory.getLogger("ErodedEnergy");

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

    public int getEnergy() {
        regenerateEnergy();
        return energy;
    }

    public int getMaxEnergy() {
        return getConfig().maxEnergy;
    }

    public boolean hasEnoughEnergy(int amount) {
        regenerateEnergy();
        return !collapsed && energy >= amount;
    }

    public void consumeEnergy(int amount) {
        if (collapsed || amount <= 0) return;

        energy = Math.max(0, energy - amount);

        if (energy == 0) {
            enterCollapse();
            return;
        }

        regenerateEnergy();
    }

    public EnergyState getEnergyState() {
        return lastEnergyState;
    }

    public void addEnergy(int amount) {
        regenerateEnergy();
        if (amount <= 0) return;

        energy = Math.min(getMaxEnergy(), energy + amount);
    }

    public void setEnergy(int value) {
        energy = Math.max(0, Math.min(getMaxEnergy(), value));
        collapsed = false;
    }

    public void setEnergyAfterDeath(float ratio) {
        int target = Math.max(0, (int) (getMaxEnergy() * ratio));
        setEnergy(target);
    }

    public void initialize() {
        energy = getMaxEnergy();
        collapsed = false;
        lastRegenTime = System.currentTimeMillis();
    }

    private void enterCollapse() {
        EnergyConfig cfg = getConfig();
        collapsed = true;
        collapseUntilMs = System.currentTimeMillis() + cfg.collapseDelayMs;

    }

    private void regenerateEnergy() {
        EnergyConfig cfg = getConfig();
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
        int restoreAmount = restoredSegments * cfg.energyPerSegment;

        if (restoreAmount > 0) {
            energy = Math.min(getMaxEnergy(), energy + restoreAmount);
            lastRegenTime += restoredSegments * intervalMs;
        }
    }

    private int lastFlashes = -1;

    public boolean areFlashesDecreasing() {
        EnergyConfig cfg = getConfig();
        int flashes = getCurrentFlashes(cfg);

        if (lastFlashes == -1) {
            lastFlashes = flashes;
            return false;
        }

        boolean decreasing = flashes < lastFlashes;
        lastFlashes = flashes;
        return decreasing;
    }

    private int getCurrentFlashes(EnergyConfig cfg) {
        float energyPerFlash = (float) getMaxEnergy() / cfg.numberEnergyFlashes;
        return (int) (energy / energyPerFlash);
    }

    private EnergyConfig getConfig() {
        return EnergyConfig.get();
    }
    public boolean tryConsumeEnergy(int amount) {
        if (!hasEnoughEnergy(amount)) return false;
        consumeEnergy(amount);
        return true;
    }
    public boolean canAffordEnergy(int amount) {
        return !collapsed && energy >= amount;
    }
}

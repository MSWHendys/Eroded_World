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

    private long immunityUntilMs = 0;

    // Persistent deterministic mining-work accumulator. It stores weighted
    // work units, not Energy, so partial mining progress survives relogs.
    private int miningWorkProgress = 0;


    public int getMiningWorkProgress() {
        return miningWorkProgress;
    }

    public void setMiningWorkProgress(int value) {
        miningWorkProgress = Math.max(0, value);
    }

    /**
     * Adds weighted mining work and returns how many whole Energy points
     * became due. The remainder is kept for the next mined block.
     */
    public int addMiningWork(int workUnits, int workUnitsPerEnergy) {
        if (workUnits <= 0 || workUnitsPerEnergy <= 0) return 0;

        long total = (long) miningWorkProgress + workUnits;
        int cost = (int) Math.min(Integer.MAX_VALUE, total / workUnitsPerEnergy);
        miningWorkProgress = (int) (total % workUnitsPerEnergy);
        return cost;
    }

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
        if (!root.server.enabled) {
            return EnergyState.NORMAL;
        }
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

    public void setImmunity(int seconds) {
        if (!EnergyConfig.get().server.enabled) return;
        this.immunityUntilMs = System.currentTimeMillis() + (seconds * 1000L);
    }

    public long getImmunityUntilMs() {
        if (immunityUntilMs <= System.currentTimeMillis()) {
            immunityUntilMs = 0L;
        }
        return immunityUntilMs;
    }

    public boolean isCollapsed() {
        regenerateEnergy();
        return collapsed;
    }

    public long getCollapseUntilMs() {
        regenerateEnergy();
        return collapsed ? collapseUntilMs : 0L;
    }

    /**
     * Restores the persisted time-based energy state after login/restart.
     * Expired timers are normalized immediately so a reconnect cannot bypass
     * collapse and an old timer cannot leave the player stuck at zero energy.
     */
    public void restoreTemporalState(boolean wasCollapsed, long persistedCollapseUntilMs, long persistedImmunityUntilMs) {
        long now = System.currentTimeMillis();

        immunityUntilMs = persistedImmunityUntilMs > now
                ? persistedImmunityUntilMs
                : 0L;

        if (wasCollapsed && persistedCollapseUntilMs > now) {
            collapsed = true;
            collapseUntilMs = persistedCollapseUntilMs;
            immunityUntilMs = 0L;
        } else {
            collapsed = false;
            collapseUntilMs = 0L;

            // If a persisted collapse elapsed while the player/server was
            // offline, finish the same recovery that regenerateEnergy() would
            // have performed online.
            if (wasCollapsed && energy <= 0) {
                energy = Math.min(getMaxEnergy(), 1);
            }
        }

        lastRegenTime = now;
        lastEnergyState = calculateEnergyState();
    }

    public boolean isImmune() {
        return EnergyConfig.get().server.enabled
                && System.currentTimeMillis() < immunityUntilMs;
    }

    public long getImmunityRemainingMs() {
        if (!EnergyConfig.get().server.enabled) return 0;
        return Math.max(0, immunityUntilMs - System.currentTimeMillis());
    }


    public boolean hasEnoughEnergy(int amount) {
        if (!EnergyConfig.get().server.enabled) return true;
        regenerateEnergy();

        if (isImmune() && !collapsed) return true;
        return !collapsed && energy >= amount;
    }

    public boolean canAffordEnergy(int amount) {
        if (!EnergyConfig.get().server.enabled) return true;
        regenerateEnergy();
        if (isImmune() && !collapsed) return true;
        return !collapsed && energy >= amount;
    }

    public boolean tryConsumeEnergy(int amount) {
        if (!hasEnoughEnergy(amount)) return false;
        consumeEnergy(amount);
        return true;
    }

    public void consumeEnergy(int amount) {

        if (!EnergyConfig.get().server.enabled) return;
        if (isImmune()) return;

        if (collapsed || amount <= 0) return;

        regenerateEnergy();

        energy = Math.max(0, energy - amount);

        if (energy == 0) {
            enterCollapse();
            return;
        }
    }

    public void addEnergy(int amount) {
        if (!EnergyConfig.get().server.enabled) return;
        regenerateEnergy();
        if (amount <= 0) return;

        energy = Math.min(getMaxEnergy(), energy + amount);

        if (energy > 0) {
            collapsed = false;
            collapseUntilMs = 0L;
        }

        lastEnergyState = calculateEnergyState();
    }

    public void setEnergy(int value) {
        energy = Math.max(0, Math.min(getMaxEnergy(), value));

        // Part 8B.1: zero Energy must have one meaning regardless of how it
        // was reached. Previously /eroded energy <player> 0 explicitly cleared
        // collapse, while normal Energy consumption entered collapse.
        if (EnergyConfig.get().server.enabled && energy == 0) {
            enterCollapse();
        } else {
            collapsed = false;
            collapseUntilMs = 0L;
        }

        lastEnergyState = calculateEnergyState();
    }

    public void setEnergyAfterDeath(float ratio) {
        int target = Math.max(0, (int) (getMaxEnergy() * ratio));
        setEnergy(target);
        // Death ends temporary adrenaline protection. Otherwise a player could
        // die and respawn with the old energy-drain immunity still active.
        immunityUntilMs = 0L;
    }

    public void initialize() {
        energy = getMaxEnergy();
        collapsed = false;
        collapseUntilMs = 0L;
        lastRegenTime = System.currentTimeMillis();
        immunityUntilMs = 0L;
        miningWorkProgress = 0;

        lastEnergyState = calculateEnergyState();
    }

    private void enterCollapse() {
        var energyRoot = EnergyConfig.get();
        var cfg = energyRoot.server.collapse;
        collapsed = true;
        collapseUntilMs = System.currentTimeMillis() + cfg.collapseDelayMs;
        immunityUntilMs = 0;
    }

    public void pauseRegenerationClock() {
        lastRegenTime = System.currentTimeMillis();
    }

    private void regenerateEnergy() {
        var root = EnergyConfig.get();
        long now = System.currentTimeMillis();

        if (!root.server.enabled) {
            lastRegenTime = now;
            return;
        }

        var cfg = root.server.regen;

        // A zero-Energy state must never remain as a normal walking state.
        // This also normalizes old saves or Energy set to 0 while the master
        // switch was disabled once Energy is enabled again.
        if (!collapsed && energy <= 0) {
            enterCollapse();
            return;
        }

        if (collapsed) {
            if (now < collapseUntilMs) {
                return;
            }
            energy = 1;
            collapsed = false;
            collapseUntilMs = 0L;
            lastRegenTime = now;
            return;
        }

        if (!cfg.passiveRegenEnabled) {
            lastRegenTime = now;
            return;
        }

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

    public int getLevel(SkillType type) {
        float cg = getCg(type);

        return Math.min(10, Math.max(0, (int) (cg / 10.0f)));
    }

    public boolean hasLevel(SkillType type, int requiredLevel) {
        return getLevel(type) >= requiredLevel;
    }

    public Map<SkillType, Float> getAllCg() {
        return cgMap;
    }

    public void setAllCg(Map<SkillType, Float> map) {
        cgMap.clear();
        cgMap.putAll(map);
    }

    private EnergyConfig getConfig() {
        return EnergyConfig.get();
    }
}

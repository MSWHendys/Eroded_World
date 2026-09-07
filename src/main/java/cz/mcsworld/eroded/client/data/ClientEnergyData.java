package cz.mcsworld.eroded.client.data;

public final class ClientEnergyData {

    private static boolean enabled = true;
    private static int energy = 100;
    private static int maxEnergy = 100;
    private static int immunitySeconds = 0;
    private static boolean miningEnabled = true;
    private static boolean miningSpeedScalingEnabled = true;
    private static boolean miningAllowAtZero = true;
    private static int miningFullSpeedFromPercent = 51;
    private static int miningReducedSpeedFromPercent = 21;
    private static int miningReducedSpeedPercent = 50;
    private static int miningCriticalSpeedPercent = 10;
    private static boolean initialized = false;

    private ClientEnergyData() {}

    public static void update(
            boolean enabled,
            int energy,
            int maxEnergy,
            int immunitySeconds,
            boolean miningEnabled,
            boolean miningSpeedScalingEnabled,
            boolean miningAllowAtZero,
            int miningFullSpeedFromPercent,
            int miningReducedSpeedFromPercent,
            int miningReducedSpeedPercent,
            int miningCriticalSpeedPercent
    ) {
        ClientEnergyData.enabled = enabled;
        ClientEnergyData.energy = energy;
        ClientEnergyData.maxEnergy = maxEnergy;
        ClientEnergyData.immunitySeconds = immunitySeconds;
        ClientEnergyData.miningEnabled = miningEnabled;
        ClientEnergyData.miningSpeedScalingEnabled = miningSpeedScalingEnabled;
        ClientEnergyData.miningAllowAtZero = miningAllowAtZero;
        ClientEnergyData.miningFullSpeedFromPercent = miningFullSpeedFromPercent;
        ClientEnergyData.miningReducedSpeedFromPercent = miningReducedSpeedFromPercent;
        ClientEnergyData.miningReducedSpeedPercent = miningReducedSpeedPercent;
        ClientEnergyData.miningCriticalSpeedPercent = miningCriticalSpeedPercent;
        initialized = true;
    }

    public static boolean isEnabled() { return enabled; }
    public static int getEnergy() { return energy; }
    public static int getMaxEnergy() { return maxEnergy; }
    public static int getImmunitySeconds() { return immunitySeconds; }
    public static boolean isImmune() { return immunitySeconds > 0; }
    public static boolean isInitialized() { return initialized; }
    public static boolean isMiningEnabled() { return miningEnabled; }
    public static boolean isMiningSpeedScalingEnabled() { return miningSpeedScalingEnabled; }
    public static boolean isMiningAllowAtZero() { return miningAllowAtZero; }
    public static int getMiningFullSpeedFromPercent() { return miningFullSpeedFromPercent; }
    public static int getMiningReducedSpeedFromPercent() { return miningReducedSpeedFromPercent; }
    public static int getMiningReducedSpeedPercent() { return miningReducedSpeedPercent; }
    public static int getMiningCriticalSpeedPercent() { return miningCriticalSpeedPercent; }
}

package cz.mcsworld.eroded.client.data;

public final class ClientEnergyData {

    private static int energy = 100;
    private static int maxEnergy = 100;
    private static int immunitySeconds = 0;
    private static boolean initialized = false;

    private ClientEnergyData() {}

    public static void update(int energy, int maxEnergy, int immunitySeconds) {
        ClientEnergyData.energy = energy;
        ClientEnergyData.maxEnergy = maxEnergy;
        ClientEnergyData.immunitySeconds = immunitySeconds;
        initialized = true;
    }

    public static int getEnergy() {
        return energy;
    }

    public static int getMaxEnergy() {
        return maxEnergy;
    }

    public static int getImmunitySeconds() {
        return immunitySeconds;
    }

    public static boolean isImmune() {
        return immunitySeconds > 0;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
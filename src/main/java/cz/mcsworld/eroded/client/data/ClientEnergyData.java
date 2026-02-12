package cz.mcsworld.eroded.client.data;

public final class ClientEnergyData {

    private static int energy = 100;
    private static int maxEnergy = 100;
    private static boolean initialized = false;
    private ClientEnergyData() {}

    public static void update(int energy, int maxEnergy) {
        ClientEnergyData.energy = energy;
        ClientEnergyData.maxEnergy = maxEnergy;
        initialized = true;
    }

    public static int getEnergy() {
        return energy;
    }

    public static int getMaxEnergy() {
        return maxEnergy;
    }
    public static boolean isInitialized() {
        return initialized;
    }

}

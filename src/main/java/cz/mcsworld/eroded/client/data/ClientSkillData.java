package cz.mcsworld.eroded.client.data;

public class ClientSkillData {

    private static float woodworking;
    private static float smelting;

    public static void update(float wood, float smelt) {
        woodworking = wood;
        smelting = smelt;
    }

    public static float getWoodworking() {
        return woodworking;
    }

    public static float getSmelting() {
        return smelting;
    }
}
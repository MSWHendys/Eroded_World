package cz.mcsworld.eroded.config.energy;

import net.minecraft.network.chat.Component;

public enum EnergyHudPosition {
    CENTER_DOWN,
    LEFT_DOWN,
    RIGHT_DOWN,
    CENTER_UP,
    LEFT_UP,
    RIGHT_UP;


    public Component getTranslation() {
        return Component.translatable(
                "eroded.energy.hud.position." + name().toLowerCase()
        );
    }
}

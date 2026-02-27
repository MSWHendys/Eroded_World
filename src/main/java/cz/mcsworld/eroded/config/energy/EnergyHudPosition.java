package cz.mcsworld.eroded.config.energy;

import net.minecraft.text.Text;

public enum EnergyHudPosition {
    CENTER_DOWN,
    LEFT_DOWN,
    RIGHT_DOWN,
    CENTER_UP,
    LEFT_UP,
    RIGHT_UP;


    public Text getTranslation() {
        return Text.translatable(
                "eroded.energy.hud.position." + name().toLowerCase()
        );
    }
}

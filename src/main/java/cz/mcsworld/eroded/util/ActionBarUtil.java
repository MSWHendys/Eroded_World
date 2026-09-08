package cz.mcsworld.eroded.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ActionBarUtil {

    public static void send(ServerPlayer player, Component text) {

        player.sendSystemMessage(text, true);
    }
}

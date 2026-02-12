package cz.mcsworld.eroded.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ActionBarUtil {

    public static void send(ServerPlayerEntity player, Text text) {

        player.sendMessage(text, true);
    }
}

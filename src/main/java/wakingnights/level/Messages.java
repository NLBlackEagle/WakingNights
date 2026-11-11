package wakingnights.level;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import wakingnights.handlers.ForgeConfigHandler;

/** Sends Waking Night messages to the action bar and/or chat, as configured */
public final class Messages {

    private Messages() {}

    public static boolean actionBar() {
        return ForgeConfigHandler.server.actionBarMessages;
    }

    public static boolean chat() {
        return ForgeConfigHandler.server.chatMessages;
    }

    public static void send(EntityPlayerMP player, ITextComponent message) {
        if(actionBar()) player.sendStatusMessage(message, true);
        if(chat()) player.sendMessage(message);
    }
}

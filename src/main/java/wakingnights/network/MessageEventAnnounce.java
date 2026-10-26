package wakingnights.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import wakingnights.client.EventBanner;

import javax.annotation.Nullable;

/**
 * Server -> client, at nightfall: a Waking Night truly starts. The client shows the night type's banner and plays
 * its start sound.
 */
public class MessageEventAnnounce implements IMessage {
    private String nightType;
    private String sound;

    public MessageEventAnnounce() {}

    public MessageEventAnnounce(@Nullable String nightType, @Nullable String sound) {
        this.nightType = nightType == null ? "" : nightType;
        this.sound = sound == null ? "" : sound;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.nightType = ByteBufUtils.readUTF8String(buf);
        this.sound = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.nightType);
        ByteBufUtils.writeUTF8String(buf, this.sound);
    }

    public static class Handler implements IMessageHandler<MessageEventAnnounce, IMessage> {
        @Override
        public IMessage onMessage(MessageEventAnnounce message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> EventBanner.show(message.nightType, message.sound));
            return null;
        }
    }
}

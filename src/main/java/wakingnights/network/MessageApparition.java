package wakingnights.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import wakingnights.client.Apparitions;

/**
 * Server -> client, debug command: show an apparition right away
 */
public class MessageApparition implements IMessage {
    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<MessageApparition, IMessage> {
        @Override
        public IMessage onMessage(MessageApparition message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(Apparitions::forceSpawn);
            return null;
        }
    }
}

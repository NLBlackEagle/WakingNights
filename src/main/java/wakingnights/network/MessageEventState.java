package wakingnights.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import wakingnights.client.EventVisuals;
import wakingnights.event.NightType;

import javax.annotation.Nullable;

/**
 * Server -> client: whether a Waking Night is going on, and the colours its sky, fog and lightning turn to
 */
public class MessageEventState implements IMessage {
    private boolean active;
    private int sky, fog, clouds, lightning;
    private int light = 0xFFFFFF;
    private int moon = 0xFFFFFF;

    public MessageEventState() {}

    public MessageEventState(@Nullable NightType type, boolean active) {
        this.active = active;
        if(type != null) {
            this.sky = type.sky;
            this.fog = type.fog;
            this.clouds = type.clouds;
            this.lightning = type.lightning;
            this.light = type.light;
            this.moon = type.moon;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.active = buf.readBoolean();
        this.sky = buf.readInt();
        this.fog = buf.readInt();
        this.clouds = buf.readInt();
        this.lightning = buf.readInt();
        this.light = buf.readInt();
        this.moon = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.active);
        buf.writeInt(this.sky);
        buf.writeInt(this.fog);
        buf.writeInt(this.clouds);
        buf.writeInt(this.lightning);
        buf.writeInt(this.light);
        buf.writeInt(this.moon);
    }

    public static class Handler implements IMessageHandler<MessageEventState, IMessage> {
        @Override
        public IMessage onMessage(MessageEventState message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> EventVisuals.set(message.active, message.sky, message.fog, message.clouds, message.lightning, message.light, message.moon));
            return null;
        }
    }
}

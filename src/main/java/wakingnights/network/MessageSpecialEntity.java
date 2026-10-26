package wakingnights.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import wakingnights.capability.CapabilitySpecialEntityHandler;
import wakingnights.capability.ICapabilitySpecialEntity;
import wakingnights.capability.SpecialType;

/**
 * Server -> client: which kind of special an entity is, so the client knows which growths to render
 */
public class MessageSpecialEntity implements IMessage {
    private int entityId;
    private SpecialType type;

    public MessageSpecialEntity() {}

    public MessageSpecialEntity(Entity entity, SpecialType type) {
        this.entityId = entity.getEntityId();
        this.type = type;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.type = SpecialType.byOrdinal(buf.readByte());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeByte(this.type.ordinal());
    }

    public static class Handler implements IMessageHandler<MessageSpecialEntity, IMessage> {
        @Override
        public IMessage onMessage(MessageSpecialEntity message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if(Minecraft.getMinecraft().world == null) return;
                Entity entity = Minecraft.getMinecraft().world.getEntityByID(message.entityId);
                if(entity == null) return;
                ICapabilitySpecialEntity cap = entity.getCapability(CapabilitySpecialEntityHandler.CAP_SPECIAL_ENTITY, null);
                if(cap != null) cap.setType(message.type);
            });
            return null;
        }
    }
}

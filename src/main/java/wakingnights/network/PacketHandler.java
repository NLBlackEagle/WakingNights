package wakingnights.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import wakingnights.WakingNights;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(WakingNights.MODID);

    public static void init() {
        INSTANCE.registerMessage(MessageSpecialEntity.Handler.class, MessageSpecialEntity.class, 0, Side.CLIENT);
        INSTANCE.registerMessage(MessageEventState.Handler.class, MessageEventState.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(MessageApparition.Handler.class, MessageApparition.class, 2, Side.CLIENT);
        INSTANCE.registerMessage(MessageEventAnnounce.Handler.class, MessageEventAnnounce.class, 3, Side.CLIENT);
    }
}

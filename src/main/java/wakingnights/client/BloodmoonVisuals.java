package wakingnights.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import wakingnights.WakingNights;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Bloodmoon (lumien) tints the sky, moon and light red on its own, on top of ours: it hooks the end of the same sky
 * colour method, so it would win. All of that runs off one client-side switch, its ClientBloodmoonHandler's
 * "bloodmoon active", which the server turns on. While a Waking Night's colours show, it's kept off here, so Bloodmoon's
 * visuals fade out as at the end of a bloodmoon and ours are the only ones. Its spawns and sleep rules are server side
 * and stay. Looked up at runtime, Bloodmoon isn't a build dependency.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID, value = Side.CLIENT)
public final class BloodmoonVisuals {
    private static final String HANDLER = "lumien.bloodmoon.client.ClientBloodmoonHandler";

    private static boolean checked = false;
    private static Field instance;
    private static Method isActive;
    private static Method setActive;

    private BloodmoonVisuals() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        if(mc.world == null || EventVisuals.getProgress(mc.world, 0.0F) <= 0.0F || !lookUp()) return;
        try {
            Object handler = instance.get(null);
            if(handler != null && (Boolean) isActive.invoke(handler)) setActive.invoke(handler, false);
        }
        catch(ReflectiveOperationException | RuntimeException e) {
            instance = null;
            WakingNights.LOGGER.error("Couldn't turn off Bloodmoon's sky colours, they'll mix with the Waking Night's", e);
        }
    }

    /** Finds Bloodmoon's client handler once, false if Bloodmoon isn't there (or its internals changed) */
    private static boolean lookUp() {
        if(!checked) {
            checked = true;
            if(!Loader.isModLoaded("bloodmoon")) return false;
            try {
                Class<?> handlerClass = Class.forName(HANDLER);
                instance = handlerClass.getField("INSTANCE");
                isActive = handlerClass.getMethod("isBloodmoonActive");
                setActive = handlerClass.getMethod("setBloodmoon", boolean.class);
            }
            catch(ReflectiveOperationException | RuntimeException e) {
                instance = null;
                WakingNights.LOGGER.error("Couldn't find Bloodmoon's client handler, its sky colours will mix with the Waking Night's", e);
            }
        }
        return instance != null;
    }
}

package wakingnights.event;

import net.minecraft.world.World;
import wakingnights.WakingNights;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Starts a bloodmoon (lumien's Bloodmoon 1.5.x) along with the Waking Night.
 * Bloodmoon's public force() only takes effect at exactly time 12000, on Bloodmoon's own tick check, so a Waking Night
 * starting at the same tick could miss it and get its bloodmoon a night late. The private setBloodmoon(true) starts one
 * right away (and without the "a blood moon is rising" message), so that's called through reflection.
 * Bloodmoon ends a bloodmoon by itself once it's day; end() stops it at dawn already.
 * Looked up at runtime, Bloodmoon isn't a build dependency.
 */
public class BloodmoonHook implements NightHook {
    private static final String HANDLER = "lumien.bloodmoon.server.BloodmoonHandler";

    private Field instance;
    private Method setBloodmoon;
    private Method stop;
    private boolean failed = false;

    @Override
    public void start(World world) {
        Object handler = this.getHandler();
        if(handler == null) return;
        try {
            this.setBloodmoon.invoke(handler, true);
        }
        catch(ReflectiveOperationException | RuntimeException e) {
            this.fail(e);
        }
    }

    @Override
    public void end(World world) {
        Object handler = this.getHandler();
        if(handler == null) return;
        try {
            this.stop.invoke(handler);
        }
        catch(ReflectiveOperationException | RuntimeException e) {
            this.fail(e);
        }
    }

    /** BloodmoonHandler.INSTANCE, null if it isn't there (yet) or Bloodmoon's internals changed */
    private Object getHandler() {
        if(this.failed) return null;
        try {
            if(this.instance == null) {
                Class<?> handlerClass = Class.forName(HANDLER);
                this.instance = handlerClass.getField("INSTANCE");
                this.setBloodmoon = handlerClass.getDeclaredMethod("setBloodmoon", boolean.class);
                this.setBloodmoon.setAccessible(true);
                this.stop = handlerClass.getMethod("stop");
            }
            return this.instance.get(null);
        }
        catch(ReflectiveOperationException | RuntimeException e) {
            this.fail(e);
            return null;
        }
    }

    private void fail(Exception e) {
        this.failed = true;
        WakingNights.LOGGER.error("Couldn't start or stop a bloodmoon, Waking Nights won't try again until restarted", e);
    }
}

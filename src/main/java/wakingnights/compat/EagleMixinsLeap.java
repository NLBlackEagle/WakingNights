package wakingnights.compat;

import net.minecraft.entity.EntityLiving;
import net.minecraftforge.fml.common.Loader;
import wakingnights.WakingNights;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * EagleMixins only gives mobs their leap as they join the world. Mobs that get the EagleMixinsLeap tag after joining
 * (Special Mob NBT on a mob that turned special in the world) are checked again through its LeapHandler.recheck.
 * Looked up by reflection: does nothing without EagleMixins or with a version that doesn't have it.
 */
public class EagleMixinsLeap {
    private static final MethodHandle RECHECK = find();

    private static MethodHandle find() {
        if(!Loader.isModLoaded("eaglemixins")) return null;
        try {
            Class<?> handler = Class.forName("eaglemixins.handlers.LeapHandler");
            return MethodHandles.publicLookup().findStatic(handler, "recheck", MethodType.methodType(boolean.class, EntityLiving.class));
        }
        catch(ReflectiveOperationException e) {
            WakingNights.LOGGER.info("EagleMixins has no LeapHandler.recheck, mobs that turn special in the world leap once reloaded");
            return null;
        }
    }

    public static void recheck(EntityLiving mob) {
        if(RECHECK == null) return;
        try {
            RECHECK.invoke(mob);
        }
        catch(Throwable e) {
            WakingNights.LOGGER.warn("EagleMixins leap recheck failed", e);
        }
    }
}

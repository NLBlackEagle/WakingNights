package wakingnights.event;

import net.minecraft.world.World;

/**
 * Starts another mod's night along with a Waking Night (a bloodmoon, a Lycanites mob event, ...).
 * Named in a night type's config entry as hook=<id>.
 */
public interface NightHook {
    /** Nightfall, the sun is gone: the Waking Night truly starts */
    void start(World world);

    /** Dawn, or the Waking Night was ended */
    void end(World world);
}

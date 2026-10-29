package wakingnights.event;

import net.minecraft.world.World;
import wakingnights.network.MessageEventState;

/**
 * Starts another mod's night along with a Waking Night (a bloodmoon, a Lycanites mob event, ...).
 * Named in a night type's config entry as hook=<id>.
 */
public interface NightHook {
    /** Nightfall, the sun is gone: the Waking Night truly starts */
    void start(World world);

    /** Every tick from nightfall until it's ended */
    default void tick(World world) {}

    /** Lets the hook change what a client in that dimension is told about the night (e.g. during a poopparty wave) */
    default MessageEventState adjustState(MessageEventState state, int dimension) {
        return state;
    }

    /** Dawn, or the Waking Night was ended */
    void end(World world);
}

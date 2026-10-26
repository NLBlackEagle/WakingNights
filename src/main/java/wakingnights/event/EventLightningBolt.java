package wakingnights.event;

import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.world.World;

/**
 * A Waking Night's lightning: visual only, with its thunder pitched by the night type (EntityLightningBoltMixin).
 */
public class EventLightningBolt extends EntityLightningBolt {
    /** Multiplies vanilla's thunder pitch. Minecraft plays nothing lower than 0.5. */
    public final float thunderPitch;

    public EventLightningBolt(World world, double x, double y, double z, float thunderPitch) {
        super(world, x, y, z, true);
        this.thunderPitch = thunderPitch;
    }
}

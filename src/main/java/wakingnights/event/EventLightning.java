package wakingnights.event;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Lightning around every player during a Waking Night, building up while the sun sets. Visual only (no fire, no damage);
 * clients draw it in the night type's colour (RenderLightningBoltMixin).
 */
public final class EventLightning {
    //Strikes land in a ring around the player: close enough to see, not right on top of them
    private static final double MIN_DISTANCE = 16.0D;
    private static final double MAX_DISTANCE = 80.0D;

    private EventLightning() {}

    /** buildUp: share of the night type's lightning, 0 to 1 */
    public static void tick(World world, @Nullable NightType type, List<EntityPlayerMP> players, float buildUp) {
        if(type == null || type.lightningPerMinute <= 0) return;
        double chancePerTick = type.lightningPerMinute * buildUp / 1200.0D;
        Random rand = world.rand;
        for(EntityPlayerMP player : players) {
            if(player.world != world || rand.nextDouble() >= chancePerTick) continue;

            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double distance = MIN_DISTANCE + rand.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            BlockPos column = new BlockPos(player.posX + Math.cos(angle) * distance, 0, player.posZ + Math.sin(angle) * distance);
            if(!world.isBlockLoaded(column)) continue;
            BlockPos ground = world.getPrecipitationHeight(column);
            world.addWeatherEffect(new EventLightningBolt(world, ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D, type.thunderPitch));
        }
    }
}

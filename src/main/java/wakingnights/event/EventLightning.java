package wakingnights.event;

import wakingnights.level.WakingNightsWorldData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
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
    public static void tick(WakingNightsWorldData data, List<EntityPlayerMP> players, float buildUp) {
        for(EntityPlayerMP player : players) {
            //The night of the player's dimension. Without a sky (nether, end) it strikes the ground near the player.
            World world = player.world;
            String typeId = data.getType(player.dimension);
            NightType type = typeId == null ? null : NightType.get(typeId);
            if(type == null || type.lightningPerMinute <= 0) continue;
            Random rand = world.rand;
            if(rand.nextDouble() >= type.lightningPerMinute * buildUp / 1200.0D) continue;

            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double distance = MIN_DISTANCE + rand.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            BlockPos column = new BlockPos(player.posX + Math.cos(angle) * distance, 0, player.posZ + Math.sin(angle) * distance);
            if(!world.isBlockLoaded(column)) continue;
            BlockPos ground = world.provider.isSurfaceWorld() ? world.getPrecipitationHeight(column) : findGround(world, column, player.posY);
            if(ground == null) continue;
            world.addWeatherEffect(new EventLightningBolt(world, ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D, type.thunderPitch));
        }
    }

    /** Without a sky: the floor of the cave or island this column has around the player's height, null if there's none */
    @Nullable
    private static BlockPos findGround(World world, BlockPos column, double y) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(column.getX(), (int) y + 8, column.getZ());
        for(int i = 0; i < 24; i++, pos.move(EnumFacing.DOWN)) {
            if(world.isAirBlock(pos) && !world.isAirBlock(pos.down())) return pos.toImmutable();
        }
        return null;
    }
}

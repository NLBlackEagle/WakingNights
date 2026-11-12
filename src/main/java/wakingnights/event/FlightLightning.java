package wakingnights.event;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.level.WakingNightsWorldData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * During a Waking Night the storm goes after players who stay off the ground: flying (elytra, flying mounts, grapples).
 * In the air means more than Flight Strike Ground Range blocks above the highest block (water counts) of their column,
 * measured from the feet of what they ride. Under a roof they're below it, so that's ground too. In the Flight Strike
 * Cave Dimensions (a roof over everything, like the nether) it's more than that many blocks of air below them instead.
 * Every Flight Strike Stage Seconds in the air the lightning around them gets more frequent and closer. From the 4th
 * stage on every strike has the Flight Strike Hit Chance to hit them: it dismounts them (the same way EagleMixins does),
 * hurts and sets them on fire. Back near the ground the time in the air resets.
 */
public final class FlightLightning {
    /** Per stage (1 to 4): average ticks between strikes, and the closest and farthest they land, in blocks */
    private static final int[] STRIKE_TICKS = {0, 30, 20, 12, 20};
    private static final double[] MIN_DISTANCE = {0, 10.0D, 6.0D, 3.0D, 2.0D};
    private static final double[] MAX_DISTANCE = {0, 24.0D, 14.0D, 8.0D, 5.0D};
    private static final int HIT_STAGE = 4;
    /** A bolt that lands farther below than this starts in the air instead, so its column still passes the player */
    private static final double MAX_DROP = 48.0D;

    private static final Map<UUID, Integer> AIR_TICKS = new HashMap<>();

    private FlightLightning() {}

    public static void reset() {
        AIR_TICKS.clear();
    }

    /** Every tick of a Waking Night after nightfall */
    public static void tick(WakingNightsWorldData data, List<EntityPlayerMP> players) {
        if(!ForgeConfigHandler.server.flightStrikesEnabled) {
            AIR_TICKS.clear();
            return;
        }
        for(EntityPlayerMP player : players) {
            UUID id = player.getUniqueID();
            String typeId = data.getType(player.dimension);
            NightType type = typeId == null ? null : NightType.get(typeId);
            if(type == null || !player.isEntityAlive() || player.isCreative() || player.isSpectator() || isGrounded(player)) {
                AIR_TICKS.remove(id);
                continue;
            }

            int airTicks = AIR_TICKS.getOrDefault(id, 0) + 1;
            AIR_TICKS.put(id, airTicks);

            int stage = getStage(airTicks);
            if(stage <= 0) continue;
            World world = player.world;
            Random rand = world.rand;
            if(rand.nextInt(STRIKE_TICKS[stage]) != 0) continue;

            if(stage >= HIT_STAGE && rand.nextDouble() * 100.0D < ForgeConfigHandler.server.flightStrikeHitChance) {
                hit(player, type);
                //Some time before the next hit can come: they're falling now, not flying
                AIR_TICKS.put(id, stageTicks() * (HIT_STAGE - 1));
            }
            else strikeNear(player, type, stage);
        }
    }

    /** 0 below the first stage, then 1 to 4 */
    private static int getStage(int airTicks) {
        return Math.min(HIT_STAGE, airTicks / stageTicks());
    }

    private static int stageTicks() {
        return Math.max(1, ForgeConfigHandler.server.flightStrikeStageSeconds * 20);
    }

    /**
     * Within Flight Strike Ground Range blocks above the highest block of their column (or below it, under a roof),
     * going by the feet of what they ride. In cave dimensions the highest block is the roof, so there it's a solid
     * block or liquid within that range below their feet instead.
     */
    private static boolean isGrounded(EntityPlayerMP player) {
        Entity base = player.getLowestRidingEntity();
        World world = player.world;
        double range = ForgeConfigHandler.server.flightStrikeGroundRange;
        if(!isCaveDimension(player.dimension)) {
            int top = world.getPrecipitationHeight(new BlockPos(base.posX, 0, base.posZ)).getY();
            return base.posY - top <= range;
        }
        //A block at y has its top at y + 1: within range when posY - (y + 1) <= range
        int from = MathHelper.floor(base.posY);
        int to = MathHelper.ceil(base.posY - range - 1.0D);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for(int y = from; y >= to && y >= 0; y--) {
            pos.setPos(base.posX, y, base.posZ);
            Material material = world.getBlockState(pos).getMaterial();
            if(material.blocksMovement() || material.isLiquid()) return true;
        }
        return false;
    }

    private static boolean isCaveDimension(int dimension) {
        for(int caveDimension : ForgeConfigHandler.server.flightStrikeCaveDimensions) {
            if(caveDimension == dimension) return true;
        }
        return false;
    }

    private static void strikeNear(EntityPlayerMP player, NightType type, int stage) {
        World world = player.world;
        Random rand = world.rand;
        double angle = rand.nextDouble() * Math.PI * 2.0D;
        double distance = MIN_DISTANCE[stage] + rand.nextDouble() * (MAX_DISTANCE[stage] - MIN_DISTANCE[stage]);
        double x = player.posX + Math.cos(angle) * distance;
        double z = player.posZ + Math.sin(angle) * distance;
        BlockPos column = new BlockPos(x, 0, z);
        if(!world.isBlockLoaded(column)) return;
        Double y = getGroundY(world, column, player.posY);
        double bottom = y == null ? player.posY - MAX_DROP : Math.max(y, player.posY - MAX_DROP);
        world.addWeatherEffect(new EventLightningBolt(world, x, bottom, z, type.thunderPitch));
    }

    @Nullable
    private static Double getGroundY(World world, BlockPos column, double playerY) {
        if(!isCaveDimension(world.provider.getDimension())) return (double) world.getPrecipitationHeight(column).getY();
        BlockPos ground = EventLightning.findGround(world, column, playerY);
        return ground == null ? null : (double) ground.getY();
    }

    /** Dismounts them like EagleMixins does, then the bolt hits */
    private static void hit(EntityPlayerMP player, NightType type) {
        player.dismountRidingEntity();
        player.removePassengers();
        World world = player.world;
        world.addWeatherEffect(new EventLightningBolt(world, player.posX, player.posY, player.posZ, type.thunderPitch));
        float damage = (float) ForgeConfigHandler.server.flightStrikeDamage;
        if(damage > 0.0F) player.attackEntityFrom(DamageSource.LIGHTNING_BOLT, damage);
        int fireSeconds = MathHelper.clamp(ForgeConfigHandler.server.flightStrikeFireSeconds, 0, 60);
        if(fireSeconds > 0) player.setFire(fireSeconds);
    }
}

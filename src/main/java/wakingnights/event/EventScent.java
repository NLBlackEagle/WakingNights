package wakingnights.event;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.level.WakingNightsWorldData;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Once the sun is gone during a Waking Night, the night's mobs now and then catch wind of where the nearest player is:
 * its own spawns and Lycanites wave mobs (EventSpawner.EVENT_TAG) and Bloodmoon's spawns ("bloodmoonSpawned", set by
 * Bloodmoon itself). Every 1 to 5 seconds (Scent Interval, random each time) each one that isn't after anything has a
 * chance (Scent Chance percent) to head to a spot near the player: off by a share of its distance (Scent Inaccuracy),
 * so it comes closer without walking straight at them, and gets more precise the closer it is. When that spot can't be
 * reached (the player is up a tower) it walks that way at its own height instead, so they gather around the tower.
 * Each mob rolls on its own, so they trickle in instead of all coming at once. Its own AI takes over again once it
 * sees the player. Special mobs (HuntPlayerTask) already always know where the player is and are left alone.
 */
public final class EventScent {
    private static final String BLOODMOON_TAG = "bloodmoonSpawned";
    //Never closer than this to dead on, so they don't all end up at the player's feet
    private static final double MIN_OFFSET = 3.0D;
    //A path can't be much longer than the mob's follow range, so far away players are approached in stretches
    private static final double STRETCH = 24.0D;
    //When even that finds nothing (a wall, a cave), a few blocks that way still gets it moving
    private static final double SHORT_STRETCH = 8.0D;
    private static final Random RAND = new Random();
    //World time of the next roll
    private static long nextRoll = 0L;

    private EventScent() {}

    /** Called every tick once the sun is gone during a Waking Night */
    public static void tick(long now, WakingNightsWorldData data, List<EntityPlayerMP> players) {
        if(!ForgeConfigHandler.server.scentEnabled) return;
        double min = Math.min(ForgeConfigHandler.server.scentIntervalMin, ForgeConfigHandler.server.scentIntervalMax);
        double max = Math.max(ForgeConfigHandler.server.scentIntervalMin, ForgeConfigHandler.server.scentIntervalMax);
        //Waiting for the next roll, unless the time was set back or forward past it
        if(now < nextRoll && nextRoll - now <= (long) (max * 20.0D)) return;
        nextRoll = now + Math.max(1L, (long) ((min + RAND.nextDouble() * (max - min)) * 20.0D));
        double range = ForgeConfigHandler.server.scentRange;
        double chance = ForgeConfigHandler.server.scentChance / 100.0D;
        //A mob near several players rolls once per roll, not once per player
        Set<EntityLiving> rolled = new HashSet<>();
        for(EntityPlayerMP player : players) {
            if(player.isSpectator() || player.isCreative() || !player.isEntityAlive()) continue;
            //Only where a Waking Night is going on
            if(data.getType(player.dimension) == null) continue;
            List<EntityLiving> mobs = player.world.getEntitiesWithinAABB(EntityLiving.class, player.getEntityBoundingBox().grow(range),
                    EventScent::isNightMob);
            for(EntityLiving mob : mobs) {
                if(!rolled.add(mob) || RAND.nextDouble() >= chance || isBusy(mob)) continue;
                //The nearest player it can go for, not necessarily this one
                EntityPlayerMP nearest = nearest(mob, players, range);
                if(nearest != null) sendTowards(mob, nearest);
            }
        }
    }

    private static boolean isNightMob(EntityLiving mob) {
        return mob != null && !mob.isDead && (mob.getEntityData().hasKey(EventSpawner.EVENT_TAG) || mob.getEntityData().getBoolean(BLOODMOON_TAG));
    }

    /** Already after something, or a special mob that hunts on its own */
    private static boolean isBusy(EntityLiving mob) {
        EntityLivingBase target = mob.getAttackTarget();
        if(target != null && target.isEntityAlive()) return true;
        return SpecialSpawner.isHunting(mob);
    }

    private static EntityPlayerMP nearest(EntityLiving mob, List<EntityPlayerMP> players, double range) {
        EntityPlayerMP nearest = null;
        double best = range * range;
        for(EntityPlayerMP player : players) {
            if(player.world != mob.world || player.isSpectator() || player.isCreative() || !player.isEntityAlive()) continue;
            double distance = mob.getDistanceSq(player);
            if(distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    /** Heads to a spot near the player, off by a share of how far away it is */
    private static boolean sendTowards(EntityLiving mob, EntityPlayerMP player) {
        double distance = mob.getDistance(player);
        double offset = Math.max(MIN_OFFSET, distance * ForgeConfigHandler.server.scentInaccuracy);
        double angle = RAND.nextDouble() * Math.PI * 2.0D;
        double radius = RAND.nextDouble() * offset;
        double x = player.posX + Math.cos(angle) * radius;
        double z = player.posZ + Math.sin(angle) * radius;
        double y = player.posY;
        if(mob.getNavigator().tryMoveToXYZ(x, y, z, 1.0D)) return true;
        //The spot can't be reached (the player is up a tower, down a hole, far away): walk that way at its own height
        //instead, a long stretch and else a short one, so they still gather around the tower. The next whiff brings it further
        Vec3d towards = new Vec3d(x - mob.posX, 0.0D, z - mob.posZ);
        double length = towards.length();
        if(length < 1.0D) return false;
        return stretch(mob, towards, length, STRETCH) || stretch(mob, towards, length, SHORT_STRETCH);
    }

    private static boolean stretch(EntityLiving mob, Vec3d towards, double length, double max) {
        Vec3d step = towards.scale(Math.min(max, length) / length);
        return mob.getNavigator().tryMoveToXYZ(mob.posX + step.x, mob.posY, mob.posZ + step.z, 1.0D);
    }
}

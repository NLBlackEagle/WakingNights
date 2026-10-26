package wakingnights.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import wakingnights.capability.CapabilitySpecialEntityHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Special mobs freeze when watched: while a player looks straight at one (and it can see them) from further than the
 * minimum distance, it stops dead and stares back, until they look away. Up close it fights as usual.
 * Checked every tick outside the mob's AI and enforced whatever AI it has: its motion is stopped, HuntPlayerTask gives
 * it no paths, and its attacks are cancelled. As a movement task at the highest priority it also holds back the mob's
 * own walking and attacking tasks, where its AI lets it.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public class FreezeWhenWatchedTask extends EntityAIBase {
    private static final double WATCH_RANGE = 32.0D;
    //Within this many degrees of looking straight at it counts as watching it
    private static final double WATCH_ANGLE = 15.0D;
    //World time each frozen mob was last held still
    private static final Map<Entity, Long> FROZEN = new WeakHashMap<>();

    private final EntityLiving mob;

    public FreezeWhenWatchedTask(EntityLiving mob) {
        this.mob = mob;
        this.setMutexBits(1 | 2);
    }

    /** Held still this tick or the one before */
    public static boolean isFrozen(Entity mob) {
        Long last = FROZEN.get(mob);
        return last != null && mob.world.getTotalWorldTime() - last <= 1;
    }

    /** Frozen mobs can't hurt anyone, also when their own AI isn't held back by the task (mods' custom attacks) */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getTrueSource();
        if(attacker != null && !attacker.world.isRemote && isFrozen(attacker)) event.setCanceled(true);
    }

    /**
     * The watching check also runs here, outside the mob's AI: a mob busy with an attack its AI won't interrupt would
     * otherwise never start the task, and never freeze
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if(!(event.getEntityLiving() instanceof EntityLiving) || event.getEntityLiving().world.isRemote) return;
        EntityLiving mob = (EntityLiving) event.getEntityLiving();
        if(!ForgeConfigHandler.server.specialFreezeWhenWatched || !CapabilitySpecialEntityHandler.isSpecial(mob)) return;
        EntityPlayer watcher = findWatcher(mob);
        if(watcher == null) return;
        hold(mob);
        mob.getLookHelper().setLookPositionWithEntity(watcher, 30.0F, 30.0F);
    }

    /** Holds back the mob's own walking and attacking tasks while it's frozen (where its AI allows that) */
    @Override
    public boolean shouldExecute() {
        return isFrozen(this.mob);
    }

    @Override
    public void updateTask() {
        this.mob.getNavigator().clearPath();
    }

    private static void hold(EntityLiving mob) {
        FROZEN.put(mob, mob.world.getTotalWorldTime());
        mob.getNavigator().clearPath();
        mob.motionX = 0.0D;
        mob.motionZ = 0.0D;
    }

    private static EntityPlayer findWatcher(EntityLiving mob) {
        for(EntityPlayer player : mob.world.playerEntities) {
            if(isWatching(mob, player)) return player;
        }
        return null;
    }

    /** Cheapest checks first: distance, then where they look, and only then whether anything blocks the view */
    private static boolean isWatching(EntityLiving mob, EntityPlayer player) {
        if(!player.isEntityAlive() || player.isSpectator()) return false;
        double distanceSq = mob.getDistanceSq(player);
        double minDistance = ForgeConfigHandler.server.specialFreezeMinDistance;
        if(distanceSq > WATCH_RANGE * WATCH_RANGE || distanceSq < minDistance * minDistance) return false;
        Vec3d toMob = new Vec3d(mob.posX, mob.posY + mob.height * 0.5D, mob.posZ).subtract(player.getPositionEyes(1.0F)).normalize();
        if(player.getLook(1.0F).dotProduct(toMob) < Math.cos(Math.toRadians(WATCH_ANGLE))) return false;
        return mob.canEntityBeSeen(player);
    }
}

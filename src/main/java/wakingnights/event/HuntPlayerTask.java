package wakingnights.event;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import wakingnights.level.WakingNightsWorldData;

/**
 * Target task of a special mob during a Waking Night: it always knows where the nearest player is and keeps coming,
 * where a normal mob forgets a target it can't see after a few seconds. Its own AI stays active and can still switch
 * targets (another player it sees, whatever hurt it); this only steps in when it has nothing to go for.
 * When its target is too far for one path, it walks a stretch towards it and paths again. Stops once the Waking Night is over.
 */
public class HuntPlayerTask extends EntityAIBase {
    private static final double HUNT_RANGE = 128.0D;
    //A path can't be much longer than the mob's follow range, so far away players are approached in stretches
    private static final double STRETCH = 40.0D;
    //Close enough for the mob's own attack AI to take over
    private static final double CLOSE = 12.0D;
    private static final int REPATH_TICKS = 40;

    private final EntityLiving mob;
    private EntityPlayer target;
    private int repath;

    public HuntPlayerTask(EntityLiving mob) {
        this.mob = mob;
        //Target task: no mutex with the movement and attack tasks
        this.setMutexBits(0);
    }

    @Override
    public boolean shouldExecute() {
        if(!WakingNightsWorldData.get(this.mob.world).isEventActive()) return false;
        this.target = this.mob.world.getNearestAttackablePlayer(this.mob, HUNT_RANGE, HUNT_RANGE);
        return this.target != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.target != null && this.target.isEntityAlive() && !this.target.isSpectator() && !this.target.isCreative()
                && this.target.world == this.mob.world && this.mob.getDistanceSq(this.target) < HUNT_RANGE * HUNT_RANGE
                && WakingNightsWorldData.get(this.mob.world).isEventActive();
    }

    @Override
    public void startExecuting() {
        this.repath = 0;
    }

    @Override
    public void resetTask() {
        this.target = null;
    }

    @Override
    public void updateTask() {
        //The mob's own AI stays in charge: it may switch to another player it sees, or to whatever hurt it.
        //Only when it has nothing to go for (it lost sight of its target) is it pointed at the hunted player again.
        EntityLivingBase current = this.mob.getAttackTarget();
        if(current == null || !current.isEntityAlive()) {
            this.mob.setAttackTarget(this.target);
            current = this.target;
        }
        //Being watched: it stays where it is (FreezeWhenWatchedTask)
        if(FreezeWhenWatchedTask.isFrozen(this.mob) || --this.repath > 0) return;
        this.repath = REPATH_TICKS;

        if(this.mob.getDistance(current) < CLOSE || !this.mob.getNavigator().noPath()) return;
        if(this.mob.getNavigator().tryMoveToEntityLiving(current, 1.0D)) return;
        Vec3d towards = current.getPositionVector().subtract(this.mob.getPositionVector()).normalize().scale(STRETCH);
        this.mob.getNavigator().tryMoveToXYZ(this.mob.posX + towards.x, this.mob.posY + towards.y, this.mob.posZ + towards.z, 1.0D);
    }
}

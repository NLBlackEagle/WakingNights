package wakingnights.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import wakingnights.WakingNights;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Parasites that come from a Waking Night's own spawn are its own spawns too (EventSpawner.markAsOwn): EagleMixins
 * leaves them alone instead of killing them in the overworld, they go at dawn, and they roll for growths like the
 * night's other spawns.
 * SRP makes new parasites in many places: evolving and merging, carriers bursting, assimilating what they kill, gore,
 * projectiles and bombs. Rather than each of those, this keeps track of which entity is acting on the server thread:
 * the one being ticked (World.updateEntityWithOptionalForce) and the one dying (its onDeath call), and a parasite
 * spawning then (World.spawnEntity) comes from the nearest parasite among those, or the parasite that shot the
 * projectile or placed the bomb that's acting.
 * Only server side and only with SRP loaded; see the World and EntityLivingBase mixins.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public final class ParasiteLineage {
    private static final String SRP_ENTITIES = "com.dhanantry.scapeandrunparasites.entity.";
    private static final String SRP_PROJECTILES = SRP_ENTITIES + "projectile.";
    private static final boolean LOADED = Loader.isModLoaded("srparasites");
    //Entities acting right now, innermost first (a parasite dying while another one is being ticked)
    private static final Deque<Entity> ACTING = new ArrayDeque<>();
    //SRP's bomb keeps the parasite that placed it here
    private static Field bombPlacedBy;
    private static boolean bombFieldLooked;

    private ParasiteLineage() {}

    public static void push(Entity entity) {
        if(LOADED && !entity.world.isRemote) ACTING.push(entity);
    }

    public static void pop(Entity entity) {
        if(!LOADED || entity.world.isRemote) return;
        if(ACTING.peek() == entity) ACTING.pop();
        else ACTING.removeFirstOccurrence(entity);
    }

    /** Nothing acts between two server ticks: whatever an exception left behind goes */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.START) ACTING.clear();
    }

    /** A new entity is about to join the world (World.spawnEntity) */
    public static void onSpawn(Entity child) {
        if(!LOADED || ACTING.isEmpty() || child.world.isRemote || !isParasite(child)) return;
        Entity parent = findParent(child);
        if(parent == null || !parent.getEntityData().hasKey(EventSpawner.EVENT_TAG) || child.getEntityData().hasKey(EventSpawner.EVENT_TAG)) return;
        NightType type = NightType.get(parent.getEntityData().getString(EventSpawner.EVENT_TAG));
        if(type != null) EventSpawner.markAsOwn(child, type);
    }

    /** The nearest parasite acting, or the one behind a projectile or bomb that's acting; null if none is */
    @Nullable
    private static Entity findParent(Entity child) {
        for(Iterator<Entity> it = ACTING.iterator(); it.hasNext(); ) {
            Entity acting = it.next();
            if(acting == child) continue;
            Entity source = getSource(acting);
            if(source != null && isParasite(source)) return source;
        }
        return null;
    }

    /** The entity itself, or whoever shot or placed it */
    @Nullable
    private static Entity getSource(Entity entity) {
        if(entity instanceof EntityFireball) return ((EntityFireball) entity).shootingEntity;
        if(entity instanceof EntityThrowable) return ((EntityThrowable) entity).getThrower();
        if(entity.getClass().getName().startsWith(SRP_PROJECTILES)) return getBombPlacer(entity);
        return entity;
    }

    @Nullable
    private static Entity getBombPlacer(Entity entity) {
        if(!bombFieldLooked) {
            bombFieldLooked = true;
            try {
                bombPlacedBy = Class.forName(SRP_PROJECTILES + "EntityBomb").getDeclaredField("tntPlacedBy");
                bombPlacedBy.setAccessible(true);
            }
            catch(ReflectiveOperationException | RuntimeException e) {
                bombPlacedBy = null;
            }
        }
        if(bombPlacedBy == null || !bombPlacedBy.getDeclaringClass().isInstance(entity)) return null;
        try {
            return (Entity) bombPlacedBy.get(entity);
        }
        catch(ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** An SRP mob (not one of its projectiles) */
    private static boolean isParasite(Entity entity) {
        String name = entity.getClass().getName();
        return entity instanceof EntityLiving && name.startsWith(SRP_ENTITIES) && !name.startsWith(SRP_PROJECTILES);
    }
}

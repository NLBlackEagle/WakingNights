package wakingnights.event;

import com.lycanitesmobs.ExtendedWorld;
import com.lycanitesmobs.core.entity.BaseCreatureEntity;
import com.lycanitesmobs.core.mobevent.MobEvent;
import com.lycanitesmobs.core.mobevent.MobEventManager;
import com.lycanitesmobs.core.mobevent.MobEventPlayerServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.level.NightTracker;
import wakingnights.level.WakingNightsWorldData;
import wakingnights.network.MessageEventState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Lycanites Mobs world events through a Waking Night: from nightfall, 2 or 3 waves (Lycanites Waves Min/Max) of mob
 * events, each stretched to fill its share of the night: Lycanites ends an event once its tick count reaches its
 * duration, so that count is held back until the wave's time is up.
 * A wave starts in every dimension whose night type has this hook, with players in it, each with an event from that
 * dimension's whitelist (Lycanites Events Per Dimension, else Lycanites Events). Events come from a bag without repeats
 * (kept with the world): each plays once before any plays again. At dawn the ones still going are stopped (only ones
 * this started, not Lycanites' own).
 * With Lycanites Despawn Waves, a wave's mobs vanish when the next wave starts, and the last wave's at dawn. They're
 * marked with the night type and wave number (EventSpawner's tags), so ones in chunks that weren't loaded then vanish
 * when they load in again.
 * Lycanites shows its own title and plays its own sound for each wave. During a poopparty wave the sky turns brown (mustard moon)
 * with yellow lightning, back to the night's colours once it's over.
 * Lycanites is only compiled against: this class may only be used when it's loaded (NightHooks.LYCANITES).
 */
public class LycanitesHook implements NightHook {
    private final Random rand = new Random();
    //World time the next wave may start, -1 while the hook isn't running
    private long nextWave = -1;
    //Waves this night and how many started; world time the current one of ours ends, 0 if none is going
    private int wavesPlanned;
    private int wavesStarted;
    private long waveEnd;
    //Before dawn, so the last wave is over by then
    private static final int NIGHT_END_MARGIN = 200;
    //Lycanites' tick count is held this far below the event's duration
    private static final int HOLD_TICKS = 100;
    //The event this started in each dimension for the current (or last) wave: only those are stopped, and their mobs
    //(Lycanites puts the event's title on every mob it spawns) marked and removed
    private final Map<Integer, MobEvent> waveEvents = new HashMap<>();
    private long ownWave;
    private boolean poopParty = false;

    private static final String POOP_PARTY = "poopparty";
    //Sky, fog, clouds, lightning, light and moon during a poopparty wave
    private static final int POOP_SKY = 0x4A2E14, POOP_FOG = 0x3A2410, POOP_CLOUDS = 0x5A3A1C, POOP_LIGHTNING = 0xFFD800, POOP_LIGHT = 0x9C6A3A,
            POOP_MOON = 0xD4A017;

    @Override
    public void start(World world) {
        int min = ForgeConfigHandler.server.lycanitesWavesMin;
        int max = Math.max(min, ForgeConfigHandler.server.lycanitesWavesMax);
        this.wavesPlanned = min + this.rand.nextInt(max - min + 1);
        this.wavesStarted = 0;
        this.waveEnd = 0;
        this.nextWave = world.getTotalWorldTime() + ForgeConfigHandler.server.lycanitesFirstEventSeconds * 20L;
        this.waveEvents.clear();
    }

    @Override
    public void tick(World world) {
        //Not started at nightfall: the server started during the Waking Night
        if(this.nextWave < 0) this.start(world);
        long now = world.getTotalWorldTime();
        if(now % 20 == 0) {
            this.markWave(world);
            this.checkPoopParty(world);
        }

        if(this.waveEnd > 0) {
            boolean going = false;
            for(Map.Entry<Integer, MobEvent> wave : this.waveEvents.entrySet()) {
                MobEventPlayerServer running = getRunning(wave.getKey(), wave.getValue());
                if(running == null) continue;
                if(now < this.waveEnd) {
                    //Keeps going: Lycanites would end it once its ticks reach the duration
                    running.ticks = Math.min(running.ticks, Math.max(0, running.mobEvent.duration - HOLD_TICKS));
                    going = true;
                }
                else ExtendedWorld.getForWorld(running.world).stopWorldEvent();
            }
            if(going) return;
            this.waveEnd = 0;
            this.nextWave = now + ForgeConfigHandler.server.lycanitesPauseSeconds * 20L;
            return;
        }
        if(this.wavesStarted >= this.wavesPlanned || now < this.nextWave) return;
        //The night's dimensions with players and without one of Lycanites' own events going, each with an event of its own
        Map<World, MobEvent> starting = new HashMap<>();
        for(World nightWorld : this.getNightWorlds(world)) {
            ExtendedWorld extendedWorld = ExtendedWorld.getForWorld(nightWorld);
            if(extendedWorld == null || extendedWorld.serverWorldEventPlayer != null || nightWorld.playerEntities.isEmpty()) continue;
            MobEvent event = this.pickEvent(world, nightWorld.provider.getDimension());
            if(event != null) starting.put(nightWorld, event);
        }
        if(starting.isEmpty()) {
            this.nextWave = now + 100;
            return;
        }
        this.despawnWave();
        this.waveEvents.clear();
        for(Map.Entry<World, MobEvent> start : starting.entrySet()) {
            ExtendedWorld.getForWorld(start.getKey()).startWorldEvent(start.getValue());
            this.waveEvents.put(start.getKey().provider.getDimension(), start.getValue());
        }
        this.ownWave = WakingNightsWorldData.get(world).nextWave();
        this.wavesStarted++;
        this.waveEnd = now + this.getWaveLength(world);
        this.checkPoopParty(world);
    }

    /** The world event going on in a dimension if it's the one this started there, else null */
    @Nullable
    private static MobEventPlayerServer getRunning(int dimension, MobEvent event) {
        World world = DimensionManager.getWorld(dimension);
        ExtendedWorld extendedWorld = world == null ? null : ExtendedWorld.getForWorld(world);
        MobEventPlayerServer running = extendedWorld == null ? null : extendedWorld.serverWorldEventPlayer;
        return running != null && running.mobEvent == event ? running : null;
    }

    /** The loaded worlds of the dimensions whose night type has this hook (hook=lycanites) */
    private List<World> getNightWorlds(World world) {
        List<World> worlds = new ArrayList<>();
        for(Map.Entry<Integer, String> entry : WakingNightsWorldData.get(world).getDimensionTypes().entrySet()) {
            NightType type = NightType.get(entry.getValue());
            if(type == null || NightHooks.get(type.hook) != this) continue;
            World nightWorld = DimensionManager.getWorld(entry.getKey());
            if(nightWorld != null) worlds.add(nightWorld);
        }
        return worlds;
    }

    /** This wave's share of what's left of the night, after the pauses still to come */
    private long getWaveLength(World world) {
        long timeOfDay = world.getWorldTime() % 24000L;
        long left = Math.max(0, NightTracker.NIGHT_END - NIGHT_END_MARGIN - timeOfDay);
        int wavesLeft = this.wavesPlanned - this.wavesStarted + 1;
        long pauses = (wavesLeft - 1) * ForgeConfigHandler.server.lycanitesPauseSeconds * 20L;
        //At least a minute, like a Lycanites event by itself
        return Math.max(1200L, (left - pauses) / wavesLeft);
    }

    @Override
    public void end(World world) {
        this.nextWave = -1;
        this.waveEnd = 0;
        this.poopParty = false;
        for(Map.Entry<Integer, MobEvent> wave : this.waveEvents.entrySet()) {
            MobEventPlayerServer running = getRunning(wave.getKey(), wave.getValue());
            if(running != null) ExtendedWorld.getForWorld(running.world).stopWorldEvent();
        }
        this.despawnWave();
        this.waveEvents.clear();
    }

    @Override
    public MessageEventState adjustState(MessageEventState state, int dimension) {
        return this.poopParty && dimension == 0 ? state.withColors(POOP_SKY, POOP_FOG, POOP_CLOUDS, POOP_LIGHTNING, POOP_LIGHT, POOP_MOON) : state;
    }

    /** Clients get the poopparty colours when such a wave starts in the overworld (its sky), and the night's own back once it's over */
    private void checkPoopParty(World world) {
        World overworld = DimensionManager.getWorld(0);
        ExtendedWorld extendedWorld = overworld == null ? null : ExtendedWorld.getForWorld(overworld);
        boolean running = extendedWorld != null && extendedWorld.serverWorldEventPlayer != null
                && POOP_PARTY.equals(extendedWorld.serverWorldEventPlayer.mobEvent.name)
                && this.getNightWorlds(world).contains(overworld);
        if(running == this.poopParty) return;
        this.poopParty = running;
        WakingNightsWorldData data = WakingNightsWorldData.get(world);
        for(EntityPlayer player : overworld.playerEntities) {
            if(player instanceof EntityPlayerMP) NightTracker.sendState((EntityPlayerMP) player, data);
        }
    }

    /** Marks the mobs the current wave spawned so far, see EventSpawner.isStray */
    private void markWave(World anyWorld) {
        if(!ForgeConfigHandler.server.lycanitesDespawnWaves) return;
        WakingNightsWorldData data = WakingNightsWorldData.get(anyWorld);
        for(Map.Entry<Integer, MobEvent> wave : this.waveEvents.entrySet()) {
            World world = DimensionManager.getWorld(wave.getKey());
            String nightType = data.getType(wave.getKey());
            if(world == null || nightType == null) continue;
            String title = wave.getValue().title;
            for(Entity entity : world.loadedEntityList) {
                if(!(entity instanceof BaseCreatureEntity)) continue;
                BaseCreatureEntity creature = (BaseCreatureEntity) entity;
                if(creature.getEntityData().hasKey(EventSpawner.WAVE_TAG)) {
                    //Tamed since: it's someone's now and stays
                    if(creature.isTamed()) {
                        creature.getEntityData().removeTag(EventSpawner.WAVE_TAG);
                        creature.getEntityData().removeTag(EventSpawner.EVENT_TAG);
                    }
                    continue;
                }
                if(creature.isTamed() || !title.equals(creature.spawnEventType)) continue;
                entity.getEntityData().setString(EventSpawner.EVENT_TAG, nightType);
                entity.getEntityData().setLong(EventSpawner.WAVE_TAG, this.ownWave);
            }
        }
    }

    /** The loaded mobs of the last wave this started vanish in a puff of smoke, except tamed ones */
    private void despawnWave() {
        if(!ForgeConfigHandler.server.lycanitesDespawnWaves) return;
        for(Map.Entry<Integer, MobEvent> wave : this.waveEvents.entrySet()) {
            World world = DimensionManager.getWorld(wave.getKey());
            if(world == null) continue;
            String title = wave.getValue().title;
            for(Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if(!(entity instanceof BaseCreatureEntity) || entity.isDead) continue;
                BaseCreatureEntity creature = (BaseCreatureEntity) entity;
                if(!title.equals(creature.spawnEventType) || creature.isTamed()) continue;
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, entity.posX, entity.posY + entity.height * 0.5D, entity.posZ,
                        12, entity.width * 0.5D, entity.height * 0.4D, entity.width * 0.5D, 0.02D);
                entity.setDead();
            }
        }
    }

    /** A dimension's event whitelist: its own from Lycanites Events Per Dimension, else Lycanites Events */
    public static List<String> getWhitelist(int dimension) {
        String prefix = dimension + "=";
        for(String entry : ForgeConfigHandler.server.lycanitesEventsPerDimension) {
            if(!entry.trim().startsWith(prefix)) continue;
            List<String> names = new ArrayList<>();
            for(String name : entry.trim().substring(prefix.length()).split(";")) {
                if(!name.trim().isEmpty()) names.add(name.trim());
            }
            return names;
        }
        return Arrays.asList(ForgeConfigHandler.server.lycanitesEvents);
    }

    /**
     * A world event from the dimension's whitelist that Lycanites has loaded and enabled, from the bag: one not played
     * yet, or once all of those have been, they're back in the bag (without the one just played there, if there's
     * another). Null if there's none.
     */
    @Nullable
    private MobEvent pickEvent(World world, int dimension) {
        List<MobEvent> candidates = new ArrayList<>();
        for(String name : getWhitelist(dimension)) {
            MobEvent event = MobEventManager.getInstance().getMobEvent(name.trim());
            if(event != null && "world".equals(event.channel) && event.isEnabled()) candidates.add(event);
        }
        if(candidates.isEmpty()) {
            WakingNights.LOGGER.warn("No Lycanites event whitelisted for dimension {} exists, is enabled and is a world event", dimension);
            return null;
        }
        WakingNightsWorldData data = WakingNightsWorldData.get(world);
        Set<String> played = data.getPlayedEvents();
        List<MobEvent> unplayed = new ArrayList<>();
        for(MobEvent event : candidates) {
            if(!played.contains(event.name)) unplayed.add(event);
        }
        if(unplayed.isEmpty()) {
            MobEvent last = this.waveEvents.get(dimension);
            for(MobEvent event : candidates) played.remove(event.name);
            for(MobEvent event : candidates) {
                if(candidates.size() == 1 || event != last) unplayed.add(event);
            }
        }
        MobEvent event = unplayed.get(this.rand.nextInt(unplayed.size()));
        played.add(event.name);
        data.markDirty();
        return event;
    }

    /** Every mob event Lycanites has loaded, as "name (channel)" sorted by name, for /wakingnights lycanites */
    public static List<String> describeEvents() {
        List<String> lines = new ArrayList<>();
        List<String> whitelist = getWhitelist(0);
        for(MobEvent event : MobEventManager.getInstance().mobEvents.values()) {
            lines.add(String.format("%s (%s%s)%s", event.name, event.channel, event.isEnabled() ? "" : ", disabled",
                    whitelist.contains(event.name) ? " [whitelisted]" : ""));
        }
        Collections.sort(lines);
        return lines;
    }

    /**
     * Flying Lycanites creatures don't follow paths, they fly with their own navigator: sends one of those towards an
     * entity. False for anything else, which walks a normal path.
     */
    public static boolean flyTowards(Entity entity, Entity target) {
        if(!(entity instanceof BaseCreatureEntity)) return false;
        BaseCreatureEntity creature = (BaseCreatureEntity) entity;
        if(!creature.useDirectNavigator() || creature.directNavigator == null) return false;
        creature.directNavigator.setTargetPosition(target, 1.0D);
        return true;
    }

    /**
     * Lycanites creatures aren't marked hostile the vanilla way (IMob), so special mobs check this too: a hostile,
     * untamed creature that isn't someone's minion
     */
    public static boolean isHostileCreature(Entity entity) {
        if(!(entity instanceof BaseCreatureEntity)) return false;
        BaseCreatureEntity creature = (BaseCreatureEntity) entity;
        return creature.isHostile() && !creature.isTamed() && !creature.isMinion();
    }
}

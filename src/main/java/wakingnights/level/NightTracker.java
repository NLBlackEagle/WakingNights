package wakingnights.level;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import wakingnights.WakingNights;
import wakingnights.event.EventLightning;
import wakingnights.event.EventSpawner;
import wakingnights.event.FlightLightning;
import wakingnights.event.NightWeather;
import wakingnights.event.NightHook;
import wakingnights.event.NightHooks;
import wakingnights.event.NightType;
import wakingnights.event.SpecialSpawner;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.network.MessageEventAnnounce;
import wakingnights.network.MessageEventState;
import wakingnights.network.PacketHandler;
import wakingnights.potion.PotionLiquid;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Random;

/**
 * Watches the overworld clock. Each night ends in one of two ways:
 * - skipped (the clock jumps past dawn, e.g. everyone slept or Morpheus skipped it): every online player gets the skip check
 * - passed naturally: every player that was awake and active long enough drops a level
 * A Waking Night event starts at dusk (same time as Bloodmoon) when every online player is at Waking Night III:
 * its night type is picked and the sky turns while the sun sets, a warning of what's coming.
 * Once the sun is gone (nightfall) it truly starts: the title, lightning.
 * Waking Night III only ever goes away by surviving one: being active for long enough during the event night,
 * which takes the Waking Night away completely. Anyone who isn't (AFK, logged out) keeps it and faces the next one.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public class NightTracker {
    //Bloodmoon decides on its bloodmoons at exactly this time of day
    public static final long DUSK = 12000;
    //The sun is gone
    public static final long NIGHTFALL = 13000;
    //Share of the lightning there already is at dusk, it builds up to all of it at nightfall
    private static final float BUILD_UP_START = 0.1F;
    //Same window in which vanilla lets you sleep in clear weather
    public static final long NIGHT_START = 12541;
    public static final long NIGHT_END = 23460;
    private static final long DAY_LENGTH = 24000;
    //Clock jumps bigger than this count as a skip instead of time passing
    private static final long SKIP_JUMP = 20;

    private static final Random RAND = new Random();
    private static long lastTime = -1;

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if(!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) lastTime = -1;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if(event.phase != TickEvent.Phase.END || event.world.isRemote || event.world.provider.getDimension() != 0) return;
        World world = event.world;
        MinecraftServer server = world.getMinecraftServer();
        if(server == null) return;

        long time = world.getWorldTime();
        long previous = lastTime;
        lastTime = time;
        //First tick, or the time was set backwards: nothing passed
        if(previous < 0 || time < previous) return;

        WakingNightsWorldData data = WakingNightsWorldData.get(world);
        List<EntityPlayerMP> players = server.getPlayerList().getPlayers();

        if(passed(previous, time, NIGHT_END)) {
            long endedNight = timesPassed(time, NIGHT_END) - 1;
            if(data.isEventActive()) endWakingNight(data, players, endedNight);
            else if(time - previous > SKIP_JUMP) onNightSkipped(players, endedNight);
            else onNightPassed(players, endedNight);
        }

        //Only when the clock is still in that part of the night: a jump over it (sleeping through a daytime
        //thunderstorm lands the next morning) would otherwise start and announce one in the morning, and announce it
        //again at nightfall. It starts at the next dusk instead.
        if(passed(previous, time, DUSK) && isEvening(time) && !data.isEventActive() && allAtMaxLevel(players)) {
            startWakingNight(data, players);
        }
        if(data.isEventActive()) {
            if(passed(previous, time, NIGHTFALL) && isAfterNightfall(time)) announceWakingNight(data, players);
            float buildUp = getBuildUp(time);
            if(buildUp > 0.0F) EventLightning.tick(data, players, buildUp);
            if(isAfterNightfall(time)) {
                EventSpawner.tick(world, data, players);
                FlightLightning.tick(data, players);
                for(NightHook hook : getHooks(data)) hook.tick(world);
            }
        }

        long night = time / DAY_LENGTH;

        if(isNight(time) && time % 20 == 0) {
            for(EntityPlayerMP player : players) {
                if(player.isPlayerSleeping() || !ActivityTracker.isActive(player)) continue;
                if(ForgeConfigHandler.server.stayAwakeOutsideComfort && WakingLevel.isComfortable(player)) continue;
                //During a Waking Night only where it's going on: no sitting it out in another dimension
                if(data.isEventActive() && data.getType(player.dimension) == null) continue;
                WakingLevel.addAwakeTicks(player, night, 20);
            }
        }
    }

    public static boolean isNight(long time) {
        long timeOfDay = time % DAY_LENGTH;
        return timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;
    }

    /** From dusk until dawn: when a Waking Night can start */
    private static boolean isEvening(long time) {
        long timeOfDay = time % DAY_LENGTH;
        return timeOfDay >= DUSK && timeOfDay < NIGHT_END;
    }

    /** The sun is gone and it isn't dawn yet: a Waking Night going on has truly started */
    public static boolean isAfterNightfall(long time) {
        long timeOfDay = time % DAY_LENGTH;
        return timeOfDay >= NIGHTFALL && timeOfDay < NIGHT_END;
    }

    /**
     * How far a Waking Night has built up: from 10% at dusk to 100% once the sun is gone at nightfall,
     * until dawn. 0 outside that window.
     */
    private static float getBuildUp(long time) {
        long timeOfDay = time % DAY_LENGTH;
        if(timeOfDay < DUSK || timeOfDay >= NIGHT_END) return 0.0F;
        if(timeOfDay >= NIGHTFALL) return 1.0F;
        return BUILD_UP_START + (1.0F - BUILD_UP_START) * (timeOfDay - DUSK) / (float) (NIGHTFALL - DUSK);
    }

    /** Whether the clock went past this time of day, also when /time jumped over it */
    private static boolean passed(long previous, long time, long timeOfDay) {
        return timesPassed(time, timeOfDay) > timesPassed(previous, timeOfDay);
    }

    /** How many times the clock has been at this time of day, up to this time */
    private static long timesPassed(long time, long timeOfDay) {
        return Math.floorDiv(time - timeOfDay, DAY_LENGTH) + 1;
    }

    /** Whether a player was awake and active for enough of the night, a Waking Night or a normal one (own percentages) */
    private static boolean stayedAwake(EntityPlayerMP player, long night, boolean wakingNight) {
        int percentage = wakingNight ? ForgeConfigHandler.server.stayAwakePercentageWaking : ForgeConfigHandler.server.stayAwakePercentageNormal;
        int required = (int) ((NIGHT_END - NIGHT_START) * percentage / 100);
        return WakingLevel.getAwakeTicks(player, night) >= required;
    }

    /**
     * Waking up from a skipped night: a comfortable bed drops a level (not with a Restless item on, comfort doesn't
     * help against those), Liquid Dreams keeps the level where it is, and having slept while Restless adds its levels
     * (see Restless). Nothing else raises it.
     */
    public static void onNightSkipped(List<EntityPlayerMP> players, long night) {
        for(EntityPlayerMP player : players) {
            if(WakingLevel.isComfortable(player) && Restless.get(player).itemLevel <= 0) WakingLevel.dropLevel(player);
            else if(!player.isPotionActive(PotionLiquid.DREAMS)) Restless.onNightSkipped(player, night);
        }
    }

    /** What reaching a new level tells the player */
    public static void tellReached(EntityPlayerMP player, int level) {
        tell(player, new TextComponentTranslation("wakingnights.message.reached_" + level), TextFormatting.DARK_PURPLE);
    }

    private static void tell(EntityPlayerMP player, TextComponentTranslation message, TextFormatting color) {
        message.setStyle(new Style().setColor(color).setItalic(true));
        Messages.send(player, message);
    }

    public static void onNightPassed(List<EntityPlayerMP> players, long night) {
        for(EntityPlayerMP player : players) {
            int level = WakingLevel.getLevel(player);
            //Waking Night III only goes away by surviving a Waking Night
            if(level <= 0 || level >= WakingLevel.MAX_LEVEL || !stayedAwake(player, night, false)) continue;
            WakingLevel.dropLevel(player);
            //The message is about what's leaving them, so it goes by the level they had through the night
            Messages.send(player, new TextComponentTranslation("wakingnights.message.stayed_awake_" + level));
        }
    }

    private static boolean allAtMaxLevel(List<EntityPlayerMP> players) {
        if(players.isEmpty()) return false;
        for(EntityPlayerMP player : players) {
            if(WakingLevel.getLevel(player) < WakingLevel.MAX_LEVEL) return false;
        }
        return true;
    }

    /** Dusk: picks the night type, and the sky starts turning on every client. */
    public static void startWakingNight(WakingNightsWorldData data, List<EntityPlayerMP> players) {
        startWakingNight(data, players, NightType.pick(data.getLastType(), RAND));
    }

    /** The overworld's time, which every dimension's Waking Night goes by (some dimensions keep their own) */
    public static long getTime() {
        World overworld = DimensionManager.getWorld(0);
        return overworld == null ? 0 : overworld.getWorldTime();
    }

    /** The hook of a night type, null if it has none or its mod isn't loaded */
    @Nullable
    private static NightHook getHook(@Nullable String type) {
        NightType nightType = type == null ? null : NightType.get(type);
        return nightType == null ? null : NightHooks.get(nightType.hook);
    }

    /** The hooks of every night type going on somewhere, each once */
    private static Set<NightHook> getHooks(WakingNightsWorldData data) {
        Set<NightHook> hooks = new LinkedHashSet<>();
        for(String type : data.getActiveTypes()) {
            NightHook hook = getHook(type);
            if(hook != null) hooks.add(hook);
        }
        return hooks;
    }

    /** With a given night type (null: none configured) */
    public static void startWakingNight(WakingNightsWorldData data, List<EntityPlayerMP> players, @Nullable NightType type) {
        //Started again over one that's going on (debug command): stop the old one's hooked nights
        for(NightHook previous : getHooks(data)) previous.end(DimensionManager.getWorld(0));
        SpecialSpawner.reset();
        EventSpawner.reset();
        FlightLightning.reset();
        if(type == null) WakingNights.LOGGER.warn("No night types configured, the Waking Night has no night type");
        data.startEvent(type == null ? null : type.id, NightType.assignDimensions(type, RAND));
        //Started over another one (debug command): its special mobs of the wrong kind and its own spawns go
        SpecialSpawner.removeStrays(data);
        EventSpawner.removeStrays(data);
        WakingNights.LOGGER.info("A Waking Night begins: {} (per dimension: {})", type == null ? "-" : type.id, data.getDimensionTypes());
        //Weather is the overworld's: its night type's
        NightWeather.start(DimensionManager.getWorld(0), data, NightType.get(data.getType(0)));
        for(EntityPlayerMP player : players) sendState(player, data);
        for(EntityPlayerMP player : players) {
            if(player.isPlayerSleeping()) player.wakeUpPlayer(true, true, false);
        }
    }

    /** Nightfall: the sun is gone and the Waking Night truly starts. */
    public static void announceWakingNight(WakingNightsWorldData data, List<EntityPlayerMP> players) {
        for(NightHook hook : getHooks(data)) hook.start(DimensionManager.getWorld(0));
        //Clients show the banner of their dimension's night type and play its start sound
        for(EntityPlayerMP player : players) {
            String type = data.getType(player.dimension);
            if(type == null) continue;
            NightType nightType = NightType.get(type);
            PacketHandler.INSTANCE.sendTo(new MessageEventAnnounce(type, nightType == null ? null : nightType.startSound), player);
        }
    }

    /** Players joining later see the sky of a Waking Night that's going on */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if(!(event.player instanceof EntityPlayerMP)) return;
        WakingNightsWorldData data = WakingNightsWorldData.get(event.player.world);
        sendState((EntityPlayerMP) event.player, WakingNightsWorldData.get(event.player.world));
    }

    /** Another dimension: its night's sky (or none) */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if(event.player instanceof EntityPlayerMP) sendState((EntityPlayerMP) event.player, WakingNightsWorldData.get(event.player.world));
    }

    /** Tells a client about the night going on in their dimension: its colours, or that there's none there */
    public static void sendState(EntityPlayerMP player, WakingNightsWorldData data) {
        String type = data.getType(player.dimension);
        MessageEventState state = new MessageEventState(type == null ? null : NightType.get(type), type != null);
        NightHook hook = getHook(type);
        PacketHandler.INSTANCE.sendTo(hook == null ? state : hook.adjustState(state, player.dimension), player);
    }

    /**
     * Only players at Waking Night III that were active for enough of the night survived it. The rest keep III,
     * and so does anyone who logged out. Players who joined in the middle at a lower level get a normal night.
     */
    public static void endWakingNight(WakingNightsWorldData data, List<EntityPlayerMP> players, long night) {
        for(NightHook hook : getHooks(data)) hook.end(DimensionManager.getWorld(0));
        NightWeather.end(DimensionManager.getWorld(0), data);
        data.endEvent();
        FlightLightning.reset();
        SpecialSpawner.removeStrays(data);
        //The night's own spawns (e.g. parasites in the overworld) must never outlive it
        EventSpawner.removeStrays(data);
        PacketHandler.INSTANCE.sendToAll(new MessageEventState(null, false));
        List<EntityPlayerMP> normalNight = new ArrayList<>();
        for(EntityPlayerMP player : players) {
            if(WakingLevel.getLevel(player) < WakingLevel.MAX_LEVEL) {
                normalNight.add(player);
            }
            else if(stayedAwake(player, night, true)) {
                WakingLevel.survivedWakingNight(player);
                tell(player, new TextComponentTranslation("wakingnights.message.night_survived"), TextFormatting.GOLD);
            }
            else {
                tell(player, new TextComponentTranslation("wakingnights.message.night_not_survived"), TextFormatting.DARK_RED);
            }
        }
        onNightPassed(normalNight, night);
    }

    /**
     * Lowest priority so it runs after Lycanites' insomnia, which blocks sleeping too (NOT_SAFE) but shows vanilla's
     * "monsters nearby" message. This replaces it with ours, and still blocks sleeping without Lycanites.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSleep(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        EntityPlayer.SleepResult result = event.getResultStatus();
        if(player.world.isRemote || (result != null && result != EntityPlayer.SleepResult.NOT_SAFE)) return;

        boolean eventActive = WakingNightsWorldData.get(player.world).isEventActive();
        if(eventActive || WakingLevel.getLevel(player) >= WakingLevel.MAX_LEVEL) {
            event.setResult(EntityPlayer.SleepResult.OTHER_PROBLEM);
            //Vanilla only sets the spawn point when sleeping works, it shouldn't cost players their bed
            player.setSpawnChunk(event.getPos(), false, player.dimension);
            Messages.send((EntityPlayerMP) player, new TextComponentTranslation(eventActive
                    ? "wakingnights.message.cant_sleep_event"
                    : "wakingnights.message.cant_sleep"));
        }
    }
}

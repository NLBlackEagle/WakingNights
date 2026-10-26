package wakingnights.level;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;

import java.util.List;

/**
 * Watches the overworld clock. Each night ends in one of two ways:
 * - skipped (the clock jumps past dawn, e.g. everyone slept or Morpheus skipped it): every online player gets the skip check
 * - passed naturally: every player that stayed awake long enough gets a Brave Night
 * A Waking Night event starts at nightfall when every online player is at Waking Night III.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public class NightTracker {
    //Same window in which vanilla lets you sleep in clear weather
    public static final long NIGHT_START = 12541;
    public static final long NIGHT_END = 23460;
    private static final long DAY_LENGTH = 24000;
    //Clock jumps bigger than this count as a skip instead of time passing
    private static final long SKIP_JUMP = 20;

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

        if(dawnsPassed(time) > dawnsPassed(previous)) {
            long endedNight = dawnsPassed(time) - 1;
            if(data.isEventActive()) endWakingNight(data, players);
            else if(time - previous > SKIP_JUMP) onNightSkipped(players);
            else onNightPassed(players, endedNight);
        }

        if(isNight(time)) {
            long night = time / DAY_LENGTH;
            if(!data.isEventActive() && data.getLastCheckedNight() != night) {
                data.setLastCheckedNight(night);
                if(allAtMaxLevel(players)) startWakingNight(data, players);
            }

            if(time % 20 == 0) {
                for(EntityPlayerMP player : players) {
                    if(player.isPlayerSleeping()) continue;
                    if(ForgeConfigHandler.server.braveNightOutsideComfort && WakingLevel.isComfortable(player)) continue;
                    WakingLevel.addAwakeTicks(player, night, 20);
                }
            }
        }
    }

    public static boolean isNight(long time) {
        long timeOfDay = time % DAY_LENGTH;
        return timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;
    }

    /** How many dawns have happened up to this time. */
    private static long dawnsPassed(long time) {
        return Math.floorDiv(time - NIGHT_END, DAY_LENGTH) + 1;
    }

    public static void onNightSkipped(List<EntityPlayerMP> players) {
        for(EntityPlayerMP player : players) {
            if(WakingLevel.isComfortable(player)) WakingLevel.dropLevel(player);
            else WakingLevel.addNight(player);
        }
    }

    public static void onNightPassed(List<EntityPlayerMP> players, long night) {
        int required = (int) ((NIGHT_END - NIGHT_START) * ForgeConfigHandler.server.braveNightAwakePercentage / 100);
        for(EntityPlayerMP player : players) {
            if(WakingLevel.getAwakeTicks(player, night) >= required) {
                WakingLevel.dropLevel(player);
                player.sendStatusMessage(new TextComponentTranslation("wakingnights.message.brave_night"), true);
            }
        }
    }

    private static boolean allAtMaxLevel(List<EntityPlayerMP> players) {
        if(players.isEmpty()) return false;
        for(EntityPlayerMP player : players) {
            if(WakingLevel.getLevel(player) < WakingLevel.MAX_LEVEL) return false;
        }
        return true;
    }

    public static void startWakingNight(WakingNightsWorldData data, List<EntityPlayerMP> players) {
        data.setEventActive(true);
        WakingNights.LOGGER.info("The Night Wakes");
        for(EntityPlayerMP player : players) {
            if(player.isPlayerSleeping()) player.wakeUpPlayer(true, true, false);
            player.connection.sendPacket(new SPacketTitle(10, 70, 20));
            //Subtitle names the night type, added together with the night types
            player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.SUBTITLE, new TextComponentString("")));
            player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.TITLE,
                    new TextComponentTranslation("wakingnights.title.the_night_wakes").setStyle(
                            new TextComponentString("").getStyle().setColor(TextFormatting.DARK_RED))));
        }
    }

    public static void endWakingNight(WakingNightsWorldData data, List<EntityPlayerMP> players) {
        data.setEventActive(false);
        for(EntityPlayerMP player : players) {
            WakingLevel.survivedWakingNight(player);
            player.sendStatusMessage(new TextComponentTranslation("wakingnights.message.night_survived"), true);
        }
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if(player.world.isRemote || event.getResultStatus() != null) return;

        boolean eventActive = WakingNightsWorldData.get(player.world).isEventActive();
        if(eventActive || WakingLevel.getLevel(player) >= WakingLevel.MAX_LEVEL) {
            event.setResult(EntityPlayer.SleepResult.OTHER_PROBLEM);
            player.sendStatusMessage(new TextComponentTranslation(eventActive
                    ? "wakingnights.message.cant_sleep_event"
                    : "wakingnights.message.cant_sleep"), true);
        }
    }
}

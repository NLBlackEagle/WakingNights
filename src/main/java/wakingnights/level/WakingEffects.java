package wakingnights.level;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.potion.PotionWakingNight;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * What a player feels at each level:
 * I: spot sounds around them at night (a list per level, getting worse), II: plus random insomnia, III: plus no sleeping (see NightTracker)
 * and background ambience at night, which everyone hears during a Waking Night.
 * Also keeps the Waking Night status icon in sync with the level.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public class WakingEffects {
    //Duration the client shows as **:**
    private static final int ICON_DURATION = 32767;
    //Short, so it wears off soon after the level drops
    private static final int LEVEL_III_INSOMNIA_DURATION = 400;
    private static final int LEVEL_III_INSOMNIA_REFRESH = 100;

    //Tick each player hears the next background ambience at
    private static final Map<UUID, Long> NEXT_BACKGROUND = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if(player.ticksExisted % 20 != 0) return;

        int level = WakingLevel.getLevel(player);
        updateIcon(player, level);
        if(level >= WakingLevel.MAX_LEVEL && ForgeConfigHandler.server.levelThreeInsomnia) keepInsomnia(player);

        World overworld = DimensionManager.getWorld(0);
        if(overworld == null || !NightTracker.isNight(overworld.getWorldTime())) return;
        Random rand = player.getRNG();
        tickBackground(player, level, overworld, rand);

        if(level <= 0 || player.ticksExisted % ForgeConfigHandler.server.effectCheckInterval != 0) return;
        if(rand.nextDouble() < ForgeConfigHandler.server.soundChance) playSound(player, level, rand);
        if(level >= 2 && rand.nextDouble() < ForgeConfigHandler.server.insomniaChance) giveInsomnia(player);
    }

    private static void updateIcon(EntityPlayerMP player, int level) {
        PotionEffect current = player.getActivePotionEffect(PotionWakingNight.INSTANCE);
        if(level <= 0) {
            if(current != null) player.removePotionEffect(PotionWakingNight.INSTANCE);
            return;
        }
        if(current != null && current.getAmplifier() == level - 1 && current.getDuration() > ICON_DURATION - 1200) return;

        player.removePotionEffect(PotionWakingNight.INSTANCE);
        player.addPotionEffect(new PotionEffect(PotionWakingNight.INSTANCE, ICON_DURATION, level - 1, true, false));
    }

    /** A spot sound from the player's level's list, so they get worse as the level rises */
    private static void playSound(EntityPlayerMP player, int level, Random rand) {
        String[] sounds = level >= 3 ? ForgeConfigHandler.server.levelThreeSounds
                : level == 2 ? ForgeConfigHandler.server.levelTwoSounds
                : ForgeConfigHandler.server.levelOneSounds;
        if(sounds == null || sounds.length == 0) return;
        //<sound id> or <sound id>=<volume>
        String picked = sounds[rand.nextInt(sounds.length)];
        String[] entry = picked.split("=", 2);
        String sound = entry[0].trim();
        float volume = 1.0F;
        if(entry.length > 1) {
            try {
                volume = MathHelper.clamp(Float.parseFloat(entry[1].trim()), 0.0F, 1.0F);
            }
            catch(NumberFormatException e) {
                WakingNights.LOGGER.warn("Invalid volume in Waking Night sound: {}", picked);
            }
        }

        //Somewhere around the player, close enough to hear, far enough to not see the source
        double angle = rand.nextDouble() * Math.PI * 2;
        double distance = 6 + rand.nextDouble() * 6;
        double x = player.posX + Math.cos(angle) * distance;
        double z = player.posZ + Math.sin(angle) * distance;
        float pitch = 0.9F + rand.nextFloat() * 0.15F;

        player.connection.sendPacket(new SPacketCustomSound(sound, SoundCategory.AMBIENT, x, player.posY, z, volume, pitch));
    }

    /**
     * Ambience at night for players from the configured level up, and for everyone during a Waking Night.
     * Played on the player itself; the background sounds are stereo, which Minecraft never places in the world,
     * so they surround the player instead of coming from somewhere.
     */
    private static void tickBackground(EntityPlayerMP player, int level, World overworld, Random rand) {
        String sound = ForgeConfigHandler.server.backgroundSound;
        boolean event = WakingNightsWorldData.get(overworld).isEventActive();
        if(sound == null || sound.isEmpty() || (level < ForgeConfigHandler.server.backgroundMinLevel && !event)) return;

        long now = overworld.getTotalWorldTime();
        Long next = NEXT_BACKGROUND.get(player.getUniqueID());
        if(next != null && now < next) return;
        //The first one comes after a wait as well, not right as the night starts
        if(next != null) {
            player.connection.sendPacket(new SPacketCustomSound(sound, SoundCategory.AMBIENT, player.posX, player.posY, player.posZ, 1.0F, 1.0F));
        }
        int min = ForgeConfigHandler.server.backgroundMinSeconds;
        int max = Math.max(min, ForgeConfigHandler.server.backgroundMaxSeconds);
        NEXT_BACKGROUND.put(player.getUniqueID(), now + (min + rand.nextInt(max - min + 1)) * 20L);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT_BACKGROUND.remove(event.player.getUniqueID());
    }

    /** Waking Night III: insomnia for as long as the level lasts, topped up before it runs out */
    private static void keepInsomnia(EntityPlayerMP player) {
        Potion insomnia = WakingLevel.getPotion(ForgeConfigHandler.server.insomniaEffect);
        if(insomnia == null) return;
        PotionEffect current = player.getActivePotionEffect(insomnia);
        if(current != null && current.getDuration() > LEVEL_III_INSOMNIA_REFRESH) return;
        player.addPotionEffect(new PotionEffect(insomnia, LEVEL_III_INSOMNIA_DURATION, 0, true, false));
    }

    private static void giveInsomnia(EntityPlayerMP player) {
        Potion insomnia = WakingLevel.getPotion(ForgeConfigHandler.server.insomniaEffect);
        if(insomnia == null || player.isPotionActive(insomnia)) return;
        player.addPotionEffect(new PotionEffect(insomnia, ForgeConfigHandler.server.insomniaDuration, 0));
    }
}

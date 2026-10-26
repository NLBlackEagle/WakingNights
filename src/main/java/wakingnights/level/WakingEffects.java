package wakingnights.level;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.potion.PotionWakingNight;

import java.util.Random;

/**
 * What a player feels at each level:
 * I: random sounds at night, II: plus random insomnia, III: plus no sleeping (see NightTracker).
 * Also keeps the Waking Night status icon in sync with the level.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public class WakingEffects {
    //Duration the client shows as **:**
    private static final int ICON_DURATION = 32767;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if(player.ticksExisted % 20 != 0) return;

        int level = WakingLevel.getLevel(player);
        updateIcon(player, level);
        if(level <= 0 || player.ticksExisted % ForgeConfigHandler.server.effectCheckInterval != 0) return;

        World overworld = DimensionManager.getWorld(0);
        if(overworld == null || !NightTracker.isNight(overworld.getWorldTime())) return;

        Random rand = player.getRNG();
        if(rand.nextDouble() < ForgeConfigHandler.server.soundChance) playSound(player, rand);
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

    private static void playSound(EntityPlayerMP player, Random rand) {
        String[] sounds = ForgeConfigHandler.server.sounds;
        if(sounds == null || sounds.length == 0) return;
        String sound = sounds[rand.nextInt(sounds.length)];

        //Somewhere around the player, close enough to hear, far enough to not see the source
        double angle = rand.nextDouble() * Math.PI * 2;
        double distance = 6 + rand.nextDouble() * 6;
        double x = player.posX + Math.cos(angle) * distance;
        double z = player.posZ + Math.sin(angle) * distance;
        float pitch = 0.8F + rand.nextFloat() * 0.3F;

        player.connection.sendPacket(new SPacketCustomSound(sound, SoundCategory.AMBIENT, x, player.posY, z, 1.0F, pitch));
    }

    private static void giveInsomnia(EntityPlayerMP player) {
        Potion insomnia = WakingLevel.getPotion(ForgeConfigHandler.server.insomniaEffect);
        if(insomnia == null || player.isPotionActive(insomnia)) return;
        player.addPotionEffect(new PotionEffect(insomnia, ForgeConfigHandler.server.insomniaDuration, 0));
    }
}

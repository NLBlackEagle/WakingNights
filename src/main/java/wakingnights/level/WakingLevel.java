package wakingnights.level;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;

import javax.annotation.Nullable;

/**
 * A player's Waking Night counter and level. Stored in the player's persisted NBT, so it survives death.
 */
public final class WakingLevel {
    public static final int MAX_LEVEL = 3;

    private static final int[] DEFAULT_THRESHOLDS = {3, 5, 8};
    private static final int[] DEFAULT_GRACE_THRESHOLDS = {5, 9, 14};

    private static final String COUNTER = "counter";
    private static final String GRACE_DONE = "graceDone";
    private static final String AWAKE_NIGHT = "awakeNight";
    private static final String AWAKE_TICKS = "awakeTicks";

    private WakingLevel() {}

    private static NBTTagCompound getData(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        if(!entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if(!persisted.hasKey(WakingNights.MODID)) persisted.setTag(WakingNights.MODID, new NBTTagCompound());
        return persisted.getCompoundTag(WakingNights.MODID);
    }

    public static int getCounter(EntityPlayer player) {
        return getData(player).getInteger(COUNTER);
    }

    public static void setCounter(EntityPlayer player, int counter) {
        getData(player).setInteger(COUNTER, Math.max(0, counter));
    }

    public static boolean isInGrace(EntityPlayer player) {
        return !getData(player).getBoolean(GRACE_DONE);
    }

    public static void setInGrace(EntityPlayer player, boolean inGrace) {
        getData(player).setBoolean(GRACE_DONE, !inGrace);
    }

    public static int[] getThresholds(EntityPlayer player) {
        return isInGrace(player)
                ? validThresholds(ForgeConfigHandler.server.graceThresholds, DEFAULT_GRACE_THRESHOLDS)
                : validThresholds(ForgeConfigHandler.server.thresholds, DEFAULT_THRESHOLDS);
    }

    private static int[] validThresholds(int[] configured, int[] fallback) {
        if(configured == null || configured.length != MAX_LEVEL) return fallback;
        for(int i = 1; i < configured.length; i++) {
            if(configured[i] <= configured[i - 1]) return fallback;
        }
        return configured[0] > 0 ? configured : fallback;
    }

    public static int getLevel(EntityPlayer player) {
        int counter = getCounter(player);
        int[] thresholds = getThresholds(player);
        int level = 0;
        for(int i = 0; i < thresholds.length; i++) {
            if(counter >= thresholds[i]) level = i + 1;
        }
        return level;
    }

    /** An uncomfortable night: one night closer to the next level, capped at Waking Night III. */
    public static void addNight(EntityPlayer player) {
        setCounter(player, Math.min(getCounter(player) + 1, getThresholds(player)[MAX_LEVEL - 1]));
    }

    /** Drops one whole level: the counter snaps to the start of the level below. */
    public static void dropLevel(EntityPlayer player) {
        int level = getLevel(player);
        setCounter(player, level >= 2 ? getThresholds(player)[level - 2] : 0);
    }

    /** After a Waking Night event: grace is over, and one more uncomfortable night means Waking Night III again. */
    public static void survivedWakingNight(EntityPlayer player) {
        setInGrace(player, false);
        setCounter(player, getThresholds(player)[MAX_LEVEL - 1] - 1);
    }

    public static boolean isComfortable(EntityPlayer player) {
        Potion comfort = getPotion(ForgeConfigHandler.server.comfortEffect);
        if(comfort == null) return false;
        PotionEffect effect = player.getActivePotionEffect(comfort);
        //Comfort tier = amplifier + 1, the same number the game shows (Comfort IV = amplifier 3)
        return effect != null && effect.getAmplifier() + 1 >= ForgeConfigHandler.server.minComfortTier;
    }

    // ------ Brave Night: ticks spent awake during the current night ------

    public static void addAwakeTicks(EntityPlayer player, long night, int ticks) {
        NBTTagCompound data = getData(player);
        if(data.getLong(AWAKE_NIGHT) != night) {
            data.setLong(AWAKE_NIGHT, night);
            data.setInteger(AWAKE_TICKS, 0);
        }
        data.setInteger(AWAKE_TICKS, data.getInteger(AWAKE_TICKS) + ticks);
    }

    public static int getAwakeTicks(EntityPlayer player, long night) {
        NBTTagCompound data = getData(player);
        return data.getLong(AWAKE_NIGHT) == night ? data.getInteger(AWAKE_TICKS) : 0;
    }

    @Nullable
    public static Potion getPotion(String id) {
        if(id == null || id.isEmpty()) return null;
        return ForgeRegistries.POTIONS.getValue(new ResourceLocation(id));
    }
}

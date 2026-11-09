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
 * A player's Waking Night level (0 to III). Stored in the player's persisted NBT, so it survives death.
 * It only rises from Restless (sleeping or clicking a bed while Restless adds that many levels, see Restless),
 * Liquid Nightmares and the effect being applied again.
 */
public final class WakingLevel {
    public static final int MAX_LEVEL = 3;

    private static final String LEVEL = "level";
    //The night of the last time Restless raised the level: at most once per night
    private static final String RESTLESS_NIGHT = "restlessNight";
    //The last night the player was in bed
    private static final String SLEPT_NIGHT = "sleptNight";
    private static final String AWAKE_NIGHT = "awakeNight";
    private static final String AWAKE_TICKS = "awakeTicks";

    //Before levels: a night counter with thresholds, and a grace period until the first Waking Night
    private static final String OLD_COUNTER = "counter";
    private static final String OLD_GRACE_DONE = "graceDone";
    private static final int[] OLD_THRESHOLDS = {3, 5, 8};
    private static final int[] OLD_GRACE_THRESHOLDS = {5, 9, 14};

    private WakingLevel() {}

    private static NBTTagCompound getData(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        if(!entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if(!persisted.hasKey(WakingNights.MODID)) persisted.setTag(WakingNights.MODID, new NBTTagCompound());
        NBTTagCompound data = persisted.getCompoundTag(WakingNights.MODID);
        if(data.hasKey(OLD_COUNTER)) convertCounter(data);
        return data;
    }

    /** Players from before levels keep the level their night counter had */
    private static void convertCounter(NBTTagCompound data) {
        int counter = data.getInteger(OLD_COUNTER);
        int[] thresholds = data.getBoolean(OLD_GRACE_DONE) ? OLD_THRESHOLDS : OLD_GRACE_THRESHOLDS;
        int level = 0;
        for(int i = 0; i < thresholds.length; i++) {
            if(counter >= thresholds[i]) level = i + 1;
        }
        data.setInteger(LEVEL, level);
        data.removeTag(OLD_COUNTER);
        data.removeTag(OLD_GRACE_DONE);
    }

    public static int getLevel(EntityPlayer player) {
        return getData(player).getInteger(LEVEL);
    }

    public static void setLevel(EntityPlayer player, int level) {
        getData(player).setInteger(LEVEL, Math.max(0, Math.min(MAX_LEVEL, level)));
    }

    /** Levels up at once (Restless), at most Waking Night III */
    public static void addLevels(EntityPlayer player, int levels) {
        setLevel(player, getLevel(player) + levels);
    }

    /** One level up (Liquid Nightmares, the effect applied again), at most Waking Night III */
    public static void raiseLevel(EntityPlayer player) {
        addLevels(player, 1);
    }

    /** One level down. Not from Waking Night III, that one only goes away by surviving a Waking Night (see survivedWakingNight). */
    public static void dropLevel(EntityPlayer player) {
        int level = getLevel(player);
        if(level > 0 && level < MAX_LEVEL) setLevel(player, level - 1);
    }

    /** After a Waking Night event: the Waking Night is gone completely */
    public static void survivedWakingNight(EntityPlayer player) {
        setLevel(player, 0);
    }

    public static boolean isComfortable(EntityPlayer player) {
        Potion comfort = getPotion(ForgeConfigHandler.server.comfortEffect);
        if(comfort == null) return false;
        PotionEffect effect = player.getActivePotionEffect(comfort);
        //Comfort tier = amplifier + 1, the same number the game shows (Comfort IV = amplifier 3)
        return effect != null && effect.getAmplifier() + 1 >= ForgeConfigHandler.server.minComfortTier;
    }

    // ------ Restless: once per night ------

    /** Whether Restless already raised the level this night (nights are numbered by world day) */
    public static boolean isRestlessDone(EntityPlayer player, long night) {
        NBTTagCompound data = getData(player);
        return data.hasKey(RESTLESS_NIGHT) && data.getLong(RESTLESS_NIGHT) == night;
    }

    public static void setRestlessDone(EntityPlayer player, long night) {
        getData(player).setLong(RESTLESS_NIGHT, night);
    }

    public static void setSlept(EntityPlayer player, long night) {
        getData(player).setLong(SLEPT_NIGHT, night);
    }

    /** Whether the player was in bed during this night */
    public static boolean hasSlept(EntityPlayer player, long night) {
        NBTTagCompound data = getData(player);
        return data.hasKey(SLEPT_NIGHT) && data.getLong(SLEPT_NIGHT) == night;
    }

    // ------ Staying awake: ticks spent awake during the current night ------

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

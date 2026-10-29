package wakingnights.event;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import wakingnights.level.WakingNightsWorldData;

import javax.annotation.Nullable;

/**
 * A night type's weather (weather=rain or storm): it starts at dusk with the Waking Night, so it builds up during
 * sunset, and lasts until dawn. The weather from before is saved (with the world, so a restart keeps it) and put back
 * at dawn, timers and all: Minecraft's own weather carries on as if the night had paused it.
 */
public final class NightWeather {
    //Longer than any night, the old weather is back at dawn
    private static final int NIGHT_LONG = 24000;

    private NightWeather() {}

    public static void start(World world, WakingNightsWorldData data, @Nullable NightType type) {
        if(type == null || type.weather == NightType.Weather.NONE) {
            //Started over a Waking Night with weather (debug command): that weather goes
            end(world, data);
            return;
        }
        WorldInfo info = world.getWorldInfo();
        //Started over another Waking Night with weather (debug command): the weather from before that one stays saved
        if(data.getSavedWeather() == null) {
            NBTTagCompound saved = new NBTTagCompound();
            saved.setBoolean("raining", info.isRaining());
            saved.setBoolean("thundering", info.isThundering());
            saved.setInteger("rainTime", info.getRainTime());
            saved.setInteger("thunderTime", info.getThunderTime());
            saved.setInteger("cleanWeatherTime", info.getCleanWeatherTime());
            data.setSavedWeather(saved);
        }
        info.setCleanWeatherTime(0);
        info.setRainTime(NIGHT_LONG);
        info.setRaining(true);
        boolean storm = type.weather == NightType.Weather.STORM;
        info.setThunderTime(NIGHT_LONG);
        info.setThundering(storm);
    }

    /** Dawn, or the Waking Night was ended: the weather from before it, if it brought its own */
    public static void end(World world, WakingNightsWorldData data) {
        NBTTagCompound saved = data.getSavedWeather();
        if(saved == null) return;
        WorldInfo info = world.getWorldInfo();
        info.setRaining(saved.getBoolean("raining"));
        info.setThundering(saved.getBoolean("thundering"));
        info.setRainTime(saved.getInteger("rainTime"));
        info.setThunderTime(saved.getInteger("thunderTime"));
        info.setCleanWeatherTime(saved.getInteger("cleanWeatherTime"));
        data.setSavedWeather(null);
    }
}

package wakingnights.level;

import net.minecraft.nbt.NBTTagCompound;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import net.minecraftforge.common.util.Constants;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import wakingnights.WakingNights;

import javax.annotation.Nullable;

/**
 * Server-wide Waking Night state, saved with the world: the night type of the event going on (if any)
 * and of the last one, so the same night rarely repeats.
 */
public class WakingNightsWorldData extends WorldSavedData {
    private static final String NAME = WakingNights.MODID;

    private boolean eventActive = false;
    private String currentType = "";
    private String lastType = "";
    //Number of the latest Lycanites wave a Waking Night started, so mobs of older waves are known when they load in
    private long wave = 0;
    //The night type of each dimension during the event going on (dimensions none of them happen in aren't listed)
    private final Map<Integer, String> dimensionTypes = new HashMap<>();
    //How many times each night type has happened (counting the one going on), for its ramp
    private final Map<String, Integer> timesHappened = new HashMap<>();
    //Lycanites events played since the bag was last full (LycanitesHook): each plays once before any plays again
    private final Set<String> playedEvents = new HashSet<>();
    //The weather from before a Waking Night with its own weather, put back at dawn (NightWeather), null if none
    @Nullable
    private NBTTagCompound savedWeather;
    public WakingNightsWorldData(String name) {
        super(name);
    }

    public static WakingNightsWorldData get(World world) {
        MapStorage storage = world.getMapStorage();
        WakingNightsWorldData data = (WakingNightsWorldData) storage.getOrLoadData(WakingNightsWorldData.class, NAME);
        if(data == null) {
            data = new WakingNightsWorldData(NAME);
            storage.setData(NAME, data);
        }
        return data;
    }

    public boolean isEventActive() {
        return this.eventActive;
    }

    /**
     * The main night type of the event going on (the one picked; other dimensions may have their own, see getType),
     * null if there's none (or it wasn't found in the config)
     */
    @Nullable
    public String getCurrentType() {
        return this.eventActive && !this.currentType.isEmpty() ? this.currentType : null;
    }

    /** The night type going on in a dimension, null if there's no event or none happens there */
    @Nullable
    public String getType(int dimension) {
        if(!this.eventActive) return null;
        //Saved before night types per dimension existed: the main one, in the overworld
        if(this.dimensionTypes.isEmpty()) return dimension == 0 && !this.currentType.isEmpty() ? this.currentType : null;
        return this.dimensionTypes.get(dimension);
    }

    /** Every night type going on somewhere, each once */
    public Set<String> getActiveTypes() {
        if(!this.eventActive) return Collections.emptySet();
        if(this.dimensionTypes.isEmpty()) return this.currentType.isEmpty() ? Collections.emptySet() : Collections.singleton(this.currentType);
        return new HashSet<>(this.dimensionTypes.values());
    }

    public Map<Integer, String> getDimensionTypes() {
        return Collections.unmodifiableMap(this.dimensionTypes);
    }

    @Nullable
    public String getLastType() {
        return this.lastType.isEmpty() ? null : this.lastType;
    }

    public long getWave() {
        return this.wave;
    }

    /** A new Lycanites wave starts: its number */
    public long nextWave() {
        this.wave++;
        this.markDirty();
        return this.wave;
    }

    /** Changes to it are saved after markDirty() */
    public Set<String> getPlayedEvents() {
        return this.playedEvents;
    }

    @Nullable
    public NBTTagCompound getSavedWeather() {
        return this.savedWeather;
    }

    public void setSavedWeather(@Nullable NBTTagCompound weather) {
        this.savedWeather = weather;
        this.markDirty();
    }

    /** type: the main night type, dimensionTypes: the night type of each dimension. Each night type going on counts as happening once more. */
    public void startEvent(@Nullable String type, Map<Integer, String> dimensionTypes) {
        this.eventActive = true;
        this.currentType = type == null ? "" : type;
        if(type != null) this.lastType = type;
        this.dimensionTypes.clear();
        this.dimensionTypes.putAll(dimensionTypes);
        for(String active : new HashSet<>(dimensionTypes.values())) this.timesHappened.merge(active, 1, Integer::sum);
        this.markDirty();
    }

    /** How many times this night type has happened, the one going on included */
    public int getTimesHappened(String type) {
        return this.timesHappened.getOrDefault(type, 0);
    }

    public void endEvent() {
        this.eventActive = false;
        this.currentType = "";
        this.dimensionTypes.clear();
        this.markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.eventActive = nbt.getBoolean("eventActive");
        this.currentType = nbt.getString("currentType");
        this.lastType = nbt.getString("lastType");
        this.wave = nbt.getLong("wave");
        this.savedWeather = nbt.hasKey("savedWeather") ? nbt.getCompoundTag("savedWeather") : null;
        this.dimensionTypes.clear();
        NBTTagCompound dimensions = nbt.getCompoundTag("dimensionTypes");
        for(String dimension : dimensions.getKeySet()) {
            try {
                this.dimensionTypes.put(Integer.parseInt(dimension), dimensions.getString(dimension));
            }
            catch(NumberFormatException ignored) {}
        }
        this.timesHappened.clear();
        NBTTagCompound happened = nbt.getCompoundTag("timesHappened");
        for(String type : happened.getKeySet()) this.timesHappened.put(type, happened.getInteger(type));
        this.playedEvents.clear();
        NBTTagList played = nbt.getTagList("playedEvents", Constants.NBT.TAG_STRING);
        for(int i = 0; i < played.tagCount(); i++) this.playedEvents.add(played.getStringTagAt(i));    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("eventActive", this.eventActive);
        compound.setString("currentType", this.currentType);
        compound.setString("lastType", this.lastType);
        compound.setLong("wave", this.wave);
        if(this.savedWeather != null) compound.setTag("savedWeather", this.savedWeather);
        NBTTagCompound dimensions = new NBTTagCompound();
        for(Map.Entry<Integer, String> entry : this.dimensionTypes.entrySet()) dimensions.setString(String.valueOf(entry.getKey()), entry.getValue());
        compound.setTag("dimensionTypes", dimensions);
        NBTTagCompound happened = new NBTTagCompound();
        for(Map.Entry<String, Integer> entry : this.timesHappened.entrySet()) happened.setInteger(entry.getKey(), entry.getValue());
        compound.setTag("timesHappened", happened);
        NBTTagList played = new NBTTagList();
        for(String event : this.playedEvents) played.appendTag(new NBTTagString(event));
        compound.setTag("playedEvents", played);        return compound;
    }
}

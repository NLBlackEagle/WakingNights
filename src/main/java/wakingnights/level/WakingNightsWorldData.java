package wakingnights.level;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import wakingnights.WakingNights;

/**
 * Server-wide Waking Night state, saved with the world.
 */
public class WakingNightsWorldData extends WorldSavedData {
    private static final String NAME = WakingNights.MODID;

    private boolean eventActive = false;
    private long lastCheckedNight = -1;

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

    public void setEventActive(boolean eventActive) {
        this.eventActive = eventActive;
        this.markDirty();
    }

    public long getLastCheckedNight() {
        return this.lastCheckedNight;
    }

    public void setLastCheckedNight(long night) {
        this.lastCheckedNight = night;
        this.markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.eventActive = nbt.getBoolean("eventActive");
        this.lastCheckedNight = nbt.getLong("lastCheckedNight");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("eventActive", this.eventActive);
        compound.setLong("lastCheckedNight", this.lastCheckedNight);
        return compound;
    }
}

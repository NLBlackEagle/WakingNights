package wakingnights.level;

import net.minecraft.nbt.NBTTagCompound;
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

    /** The night type of the event going on, null if there's none (or it wasn't found in the config) */
    @Nullable
    public String getCurrentType() {
        return this.eventActive && !this.currentType.isEmpty() ? this.currentType : null;
    }

    @Nullable
    public String getLastType() {
        return this.lastType.isEmpty() ? null : this.lastType;
    }

    public void startEvent(@Nullable String type) {
        this.eventActive = true;
        this.currentType = type == null ? "" : type;
        if(type != null) this.lastType = type;
        this.markDirty();
    }

    public void endEvent() {
        this.eventActive = false;
        this.currentType = "";
        this.markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.eventActive = nbt.getBoolean("eventActive");
        this.currentType = nbt.getString("currentType");
        this.lastType = nbt.getString("lastType");    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("eventActive", this.eventActive);
        compound.setString("currentType", this.currentType);
        compound.setString("lastType", this.lastType);        return compound;
    }
}

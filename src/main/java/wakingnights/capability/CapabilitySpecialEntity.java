package wakingnights.capability;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

public class CapabilitySpecialEntity implements ICapabilitySpecialEntity {
    private final Entity entity;

    private boolean special = false;

    //Only for Capability.getDefaultImplementation, which should only be used if the capability is owner-agnostic (so if there's no need to store entity here)
    public CapabilitySpecialEntity(){
        this(null);
    }

    public CapabilitySpecialEntity(Entity entity) {
        this.entity = entity;
    }

    @Override
    public boolean isSpecial() {
        return this.special;
    }

    @Override
    public void setSpecial(boolean special) {
        this.special = special;
    }

    @Override
    public NBTTagCompound writeToNBT() {
        NBTTagCompound tags = new NBTTagCompound();
        tags.setBoolean("special", this.special);
        return tags;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.special = nbt.getBoolean("special");
    }
}

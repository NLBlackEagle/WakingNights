package wakingnights.capability;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

public class CapabilitySpecialEntity implements ICapabilitySpecialEntity {
    private final Entity entity;

    private SpecialType type = SpecialType.NONE;

    //Only for Capability.getDefaultImplementation, which should only be used if the capability is owner-agnostic (so if there's no need to store entity here)
    public CapabilitySpecialEntity(){
        this(null);
    }

    public CapabilitySpecialEntity(Entity entity) {
        this.entity = entity;
    }

    @Override
    public SpecialType getType() {
        return this.type;
    }

    @Override
    public void setType(SpecialType type) {
        this.type = type;
    }

    @Override
    public NBTTagCompound writeToNBT() {
        NBTTagCompound tags = new NBTTagCompound();
        tags.setByte("type", (byte) this.type.ordinal());
        return tags;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        //Mobs saved before there were types were all blooded
        if(!nbt.hasKey("type") && nbt.getBoolean("special")) this.type = SpecialType.BLOODED;
        else this.type = SpecialType.byOrdinal(nbt.getByte("type"));    }
}

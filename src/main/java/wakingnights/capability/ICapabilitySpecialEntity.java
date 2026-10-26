package wakingnights.capability;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Marks an entity as special (blooded or blighted): it gets the pulsating growths and drops special loot
 */
public interface ICapabilitySpecialEntity {
    SpecialType getType();
    void setType(SpecialType type);

    default boolean isSpecial() {
        return this.getType() != SpecialType.NONE;
    }
    NBTTagCompound writeToNBT();
    void readFromNBT(NBTTagCompound nbt);
}

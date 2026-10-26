package wakingnights.capability;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Marks an entity as special: it gets the pulsating blob and drops special loot
 */
public interface ICapabilitySpecialEntity {
    boolean isSpecial();
    void setSpecial(boolean special);

    NBTTagCompound writeToNBT();
    void readFromNBT(NBTTagCompound nbt);
}

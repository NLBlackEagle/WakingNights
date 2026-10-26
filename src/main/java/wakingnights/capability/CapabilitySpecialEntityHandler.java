package wakingnights.capability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wakingnights.WakingNights;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class handles interaction of the capability with everything else
 */
public class CapabilitySpecialEntityHandler {
    public static final ResourceLocation CAP_SPECIAL_ENTITY_KEY = new ResourceLocation(WakingNights.MODID, "special_entity");

    @CapabilityInject(ICapabilitySpecialEntity.class)
    public static Capability<ICapabilitySpecialEntity> CAP_SPECIAL_ENTITY;

    public static void registerCapability() {
        CapabilityManager.INSTANCE.register(ICapabilitySpecialEntity.class, new Storage(), CapabilitySpecialEntity::new); //Note: the factory here in the last parameter is only used in Capability.getDefaultImplementation which has no default usage. Otherwise the Provider is used
        MinecraftForge.EVENT_BUS.register(EventHandler.class);
    }

    public static boolean isSpecial(Entity entity) {
        ICapabilitySpecialEntity cap = entity.getCapability(CAP_SPECIAL_ENTITY, null);
        return cap != null && cap.isSpecial();
    }

    public static class EventHandler {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if(!WakingNights.completedLoading) return; //only needed when other mods instantiate objects during startup that would get the capability

            if(!(event.getObject() instanceof EntityLiving)) return;
            if(event.getObject().hasCapability(CAP_SPECIAL_ENTITY, null)) return;

            event.addCapability(CAP_SPECIAL_ENTITY_KEY, new Provider(event.getObject()));
        }
    }

    /**
     * creating instances of the capability
     */
    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final ICapabilitySpecialEntity instance;

        public Provider(Entity entity) {
            this.instance = new CapabilitySpecialEntity(entity);
        }

        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CAP_SPECIAL_ENTITY;
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CAP_SPECIAL_ENTITY ? CAP_SPECIAL_ENTITY.cast(instance) : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return (NBTTagCompound) CAP_SPECIAL_ENTITY.writeNBT(instance, null);
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            CAP_SPECIAL_ENTITY.readNBT(instance, null, nbt);
        }
    }

    /**
     * NBT handling for the capability (read/write)
     */
    public static class Storage implements Capability.IStorage<ICapabilitySpecialEntity> {
        @Override
        public NBTBase writeNBT(Capability<ICapabilitySpecialEntity> capability, ICapabilitySpecialEntity instance, EnumFacing side) {
            return instance.writeToNBT();
        }

        @Override
        public void readNBT(Capability<ICapabilitySpecialEntity> capability, ICapabilitySpecialEntity instance, EnumFacing side, NBTBase nbt) {
            instance.readFromNBT((NBTTagCompound) nbt);
        }
    }
}

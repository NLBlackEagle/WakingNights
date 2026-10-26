package wakingnights.mixin.vanilla;

import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wakingnights.event.EventLightningBolt;

/**
 * Pitches the thunder and impact sounds of a Waking Night's lightning (see EventLightningBolt).
 */
@Mixin(EntityLightningBolt.class)
public abstract class EntityLightningBoltMixin {
    @Redirect(method = "onUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/EntityPlayer;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V"))
    private void wakingnights$pitchThunder(World world, EntityPlayer player, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        Object self = this;
        if(self instanceof EventLightningBolt) pitch *= ((EventLightningBolt) self).thunderPitch;
        world.playSound(player, x, y, z, sound, category, volume, pitch);
    }
}

package wakingnights.mixin.vanilla;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wakingnights.event.SpecialSpawner;

/**
 * Mobs hunting a player during a Waking Night (special mobs and a night's own spawns) don't push each other: crowding
 * towards the player, they kept shoving one another into lava. They still bump into everything else.
 */
@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseMixin {
    @Inject(method = "collideWithEntity", at = @At("HEAD"), cancellable = true)
    private void wakingnights$noShoving(Entity other, CallbackInfo ci) {
        if(SpecialSpawner.isHunter(other) && SpecialSpawner.isHunter((Entity) (Object) this)) ci.cancel();
    }
}

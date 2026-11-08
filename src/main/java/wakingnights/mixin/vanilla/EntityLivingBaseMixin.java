package wakingnights.mixin.vanilla;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wakingnights.event.ParasiteLineage;
import wakingnights.event.SpecialSpawner;

@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseMixin {
    /**
     * Mobs hunting a player during a Waking Night (special mobs and a night's own spawns) don't push each other: crowding
     * towards the player, they kept shoving one another into lava. They still bump into everything else.
     */
    @Inject(method = "collideWithEntity", at = @At("HEAD"), cancellable = true)
    private void wakingnights$noShoving(Entity other, CallbackInfo ci) {
        if(SpecialSpawner.isHunter(other) && SpecialSpawner.isHunter((Entity) (Object) this)) ci.cancel();
    }

    /**
     * The entity dying is the one acting while its onDeath runs (mods' own overrides included, SRP's parasites burst
     * or turn into others there): what spawns then comes from it (see ParasiteLineage)
     */
    @Inject(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;onDeath(Lnet/minecraft/util/DamageSource;)V"))
    private void wakingnights$deathStart(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ParasiteLineage.push((Entity) (Object) this);
    }

    @Inject(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;onDeath(Lnet/minecraft/util/DamageSource;)V", shift = At.Shift.AFTER))
    private void wakingnights$deathEnd(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ParasiteLineage.pop((Entity) (Object) this);
    }
}

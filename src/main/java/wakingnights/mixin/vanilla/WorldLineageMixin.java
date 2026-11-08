package wakingnights.mixin.vanilla;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wakingnights.event.ParasiteLineage;

/**
 * Which entity is being ticked, and parasites spawning while it is: they take after the parasite that made them
 * (see ParasiteLineage). Server side only, it does nothing on clients.
 */
@Mixin(World.class)
public abstract class WorldLineageMixin {
    @Inject(method = "updateEntityWithOptionalForce", at = @At("HEAD"))
    private void wakingnights$tickStart(Entity entity, boolean forceUpdate, CallbackInfo ci) {
        ParasiteLineage.push(entity);
    }

    @Inject(method = "updateEntityWithOptionalForce", at = @At("RETURN"))
    private void wakingnights$tickEnd(Entity entity, boolean forceUpdate, CallbackInfo ci) {
        ParasiteLineage.pop(entity);
    }

    @Inject(method = "spawnEntity", at = @At("HEAD"))
    private void wakingnights$spawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ParasiteLineage.onSpawn(entity);
    }
}

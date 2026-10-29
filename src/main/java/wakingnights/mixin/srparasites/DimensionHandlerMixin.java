package wakingnights.mixin.srparasites;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wakingnights.event.EventSpawner;

/**
 * SRP's spawn check turns parasites away outside its dimension whitelist ("Mobs Blacklisted Dimensions") and in
 * dimensions whose evolution phase allows none. A Waking Night's own parasites (wakingnights_event, or marked
 * EagleMixinsOverworldAllowed like ours are) are let through; every other parasite is checked by SRP as before.
 */
@Pseudo
@Mixin(targets = "com.dhanantry.scapeandrunparasites.init.SRPSpawning$DimensionHandler", remap = false)
public abstract class DimensionHandlerMixin {
    @Inject(method = "onSpawn", at = @At("HEAD"), cancellable = true, require = 0)
    private static void wakingnights$letEventParasitesThrough(LivingSpawnEvent.CheckSpawn event, CallbackInfo ci) {
        Entity entity = event.getEntity();
        if(entity.getEntityData().hasKey(EventSpawner.EVENT_TAG) || entity.getEntityData().getBoolean(EventSpawner.OVERWORLD_ALLOWED_TAG)) ci.cancel();
    }
}

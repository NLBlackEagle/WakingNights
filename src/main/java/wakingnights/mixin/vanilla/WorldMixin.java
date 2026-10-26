package wakingnights.mixin.vanilla;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wakingnights.client.EventVisuals;

/**
 * Turns the sky and clouds to the Waking Night's colour and puts out the stars (see EventVisuals). Client side.
 */
@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void wakingnights$skyColor(Entity entity, float partialTicks, CallbackInfoReturnable<Vec3d> cir) {
        cir.setReturnValue(EventVisuals.applySky((World) (Object) this, cir.getReturnValue(), partialTicks));
    }

    @Inject(method = "getCloudColour", at = @At("RETURN"), cancellable = true)
    private void wakingnights$cloudColor(float partialTicks, CallbackInfoReturnable<Vec3d> cir) {
        cir.setReturnValue(EventVisuals.applyClouds((World) (Object) this, cir.getReturnValue(), partialTicks));
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void wakingnights$starBrightness(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(EventVisuals.applyStars((World) (Object) this, cir.getReturnValue(), partialTicks));
    }
}

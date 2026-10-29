package wakingnights.mixin.vanilla;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Redirect;
import wakingnights.client.EventVisuals;

/**
 * During a Waking Night, a strike doesn't light up the world in white: WorldProviderMixin flashes it in the
 * lightning's colour instead (see EventVisuals#applyLightmap).
 * Also tells BiomeMixin when the weather is drawn, for snowy Waking Nights.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = {"renderRainSnow", "addRainParticles"}, at = @At("HEAD"))
    private void wakingnights$startWeather(CallbackInfo ci) {
        EventVisuals.setDrawingWeather(true);
    }

    @Inject(method = {"renderRainSnow", "addRainParticles"}, at = @At("RETURN"))
    private void wakingnights$endWeather(CallbackInfo ci) {
        EventVisuals.setDrawingWeather(false);
    }

    @Redirect(method = "updateLightmap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getLastLightningBolt()I"))
    private int wakingnights$noWhiteFlash(World world) {
        return EventVisuals.getLightningColor() != null ? 0 : world.getLastLightningBolt();
    }
}

package wakingnights.mixin.vanilla;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wakingnights.client.growth.GrowthLayerHandler;

/**
 * Ends a special mob's skin tint once its model is drawn, so its layers (armour, held items, growths) aren't tinted.
 */
@Mixin(RenderLivingBase.class)
public abstract class RenderLivingBaseMixin {
    @Inject(method = "renderLayers", at = @At("HEAD"))
    private void wakingnights$stopTint(CallbackInfo ci) {
        GrowthLayerHandler.stopTint();
    }
}

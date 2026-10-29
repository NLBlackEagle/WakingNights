package wakingnights.mixin.mobends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wakingnights.client.growth.IRenderedPart;

/**
 * Mo' Bends parts draw themselves without going through ModelRenderer.render (see ModelRendererMixin), with their own
 * transforms. This records them as drawn, with the matrix right after their transform: exactly where their boxes go,
 * so growths follow the bent limbs.
 */
@Pseudo
@Mixin(targets = {"goblinbob.mobends.core.client.model.ModelPart", "goblinbob.mobends.core.client.model.ModelPartExtended"}, remap = false)
public abstract class ModelPartMixin {
    @Inject(method = "renderPart(F)V", at = @At(value = "INVOKE", target = "applyCharacterTransform(F)V", shift = At.Shift.AFTER), require = 0)
    private void wakingnights$onRenderPart(float scale, CallbackInfo ci) {
        ((IRenderedPart) this).wakingnights$onRender(true);
    }

    @Inject(method = "renderJustPart(F)V", at = @At(value = "INVOKE", target = "applyLocalTransform(F)V", shift = At.Shift.AFTER), require = 0)
    private void wakingnights$onRenderJustPart(float scale, CallbackInfo ci) {
        ((IRenderedPart) this).wakingnights$onRender(true);
    }
}

package wakingnights.mixin.vanilla;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wakingnights.client.EventVisuals;

/**
 * The moon in the Waking Night's colour (see EventVisuals#prepareMoon). Runs right after the moon texture is bound,
 * the second texture renderSky binds (the first is the sun).
 */
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin {
    @Shadow
    private WorldClient world;

    @Inject(method = "renderSky(FI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void wakingnights$moonColor(float partialTicks, int pass, CallbackInfo ci) {
        EventVisuals.prepareMoon(this.world, partialTicks);
    }
}

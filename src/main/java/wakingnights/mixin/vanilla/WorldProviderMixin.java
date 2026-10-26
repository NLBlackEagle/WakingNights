package wakingnights.mixin.vanilla;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wakingnights.client.EventVisuals;

/**
 * Forge's lightmap hook: the light outside during a Waking Night, tinted and flashing in the lightning's colour
 * (see EventVisuals#applyLightmap).
 */
@Mixin(WorldProvider.class)
public abstract class WorldProviderMixin {
    @Shadow
    protected World world;

    @Inject(method = "getLightmapColors", at = @At("RETURN"), remap = false)
    private void wakingnights$lightningFlash(float partialTicks, float sunBrightness, float skyLight, float blockLight, float[] colors, CallbackInfo ci) {
        EventVisuals.applyLightmap(this.world, sunBrightness, skyLight, colors);
    }
}

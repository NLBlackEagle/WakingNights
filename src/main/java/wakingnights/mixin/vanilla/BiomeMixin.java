package wakingnights.mixin.vanilla;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wakingnights.client.EventVisuals;

/**
 * A snowy Waking Night (snow=true): while the weather is drawn, every biome is freezing, so its rain falls as snow
 * everywhere. Only then and only on the client: grass colours, snow layers and everything else keep the real temperature.
 * Mods drawing the weather themselves (Dynamic Surroundings) or changing its temperature (Serene Seasons) still ask
 * the biome, so they snow too.
 */
@Mixin(Biome.class)
public abstract class BiomeMixin {
    @Inject(method = "getTemperature(Lnet/minecraft/util/math/BlockPos;)F", at = @At("HEAD"), cancellable = true)
    private void wakingnights$snowyNight(BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if(EventVisuals.isDrawingSnow()) cir.setReturnValue(0.0F);
    }
}

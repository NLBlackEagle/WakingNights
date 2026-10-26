package wakingnights.mixin.vanilla;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * RenderLivingBase#addLayer is protected, this lets the growth layer be added to any mob's renderer
 */
@Mixin(RenderLivingBase.class)
public interface RenderLivingBaseAccessor {
    @Accessor("layerRenderers")
    List<LayerRenderer<?>> getLayerRenderers();
}

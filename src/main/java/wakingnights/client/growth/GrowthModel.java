package wakingnights.client.growth;

import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * The model and animation of one growth shape. Textures are picked per growth by GrowthLayer,
 * so getTextureLocation is only a fallback.
 */
public class GrowthModel extends AnimatedGeoModel<GrowthAnimatable> {
    private final ResourceLocation model;
    private final ResourceLocation animation;
    private final ResourceLocation texture;

    public GrowthModel(ResourceLocation model, ResourceLocation animation, ResourceLocation texture) {
        this.model = model;
        this.animation = animation;
        this.texture = texture;
    }

    @Override
    public ResourceLocation getModelLocation(GrowthAnimatable object) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureLocation(GrowthAnimatable object) {
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(GrowthAnimatable animatable) {
        return this.animation;
    }
}
